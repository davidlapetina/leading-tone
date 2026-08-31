package fr.lapetina.music.knowledge.retrieval;

import fr.lapetina.music.knowledge.embedding.EmbeddingModelInfo;
import fr.lapetina.music.knowledge.embedding.EmbeddingService;
import fr.lapetina.music.knowledge.index.IndexFields;
import fr.lapetina.music.knowledge.index.KnowledgeIndex;
import fr.lapetina.music.knowledge.index.MusicAnalyzer;
import fr.lapetina.music.knowledge.index.MusicSymbols;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Hybrid search over the knowledge index: exact symbols, ordinary words, and meaning.
 *
 * <p>Three things are worth knowing about the implementation.
 *
 * <p><strong>No query parser touches learner text.</strong> Clauses are built from analysed
 * tokens directly, so a question containing {@code V/V} or a stray bracket is a question,
 * not a syntax error, and the resulting query is reproducible in a test.
 *
 * <p><strong>The licence filter is a filter, not a rank.</strong> Only sources whose
 * ingestion completed are searched at all, and the restriction is passed into the vector
 * query so the graph traversal respects it instead of returning fewer results than asked
 * for after the fact.
 *
 * <p><strong>It never throws into a turn.</strong> With no index, no active source or a
 * broken searcher, this returns nothing and the tutor teaches from the theory engine, the
 * same way it already copes with the language model being off.
 */
@ApplicationScoped
public class KnowledgeRetriever {

    private static final Logger LOG = Logger.getLogger(KnowledgeRetriever.class);

    @Inject
    KnowledgeIndex index;

    @Inject
    EmbeddingService embedder;

    @Inject
    fr.lapetina.music.knowledge.license.LicensePolicyService licensePolicy;

    @ConfigProperty(name = "music.knowledge.retrieval.lexical-weight", defaultValue = "0.55")
    double lexicalWeight;

    @ConfigProperty(name = "music.knowledge.retrieval.vector-weight", defaultValue = "0.45")
    double vectorWeight;

    @ConfigProperty(name = "music.knowledge.retrieval.concept-boost", defaultValue = "0.35")
    double conceptBoost;

    @ConfigProperty(name = "music.knowledge.retrieval.takeaway-boost", defaultValue = "0.20")
    double takeawayBoost;

    @ConfigProperty(name = "music.knowledge.retrieval.candidates", defaultValue = "40")
    int candidates;

    @ConfigProperty(name = "music.knowledge.retrieval.max-per-document", defaultValue = "2")
    int maxPerDocument;

    public RetrievalResult retrieve(RetrievalQuery query) {
        if (query == null || query.isBlank() || !index.isOpen()) {
            return RetrievalResult.EMPTY;
        }
        Set<String> allowed = retrievableSourceIds();
        if (allowed.isEmpty()) {
            return RetrievalResult.EMPTY;
        }
        long started = System.currentTimeMillis();
        try {
            return search(query, allowed, started);
        } catch (RuntimeException e) {
            LOG.warnf("Retrieval failed, continuing without it: %s", e.toString());
            return RetrievalResult.EMPTY;
        }
    }

    /**
     * Delegated, not reimplemented. The licence decision lives in one place so it cannot
     * differ between the routes that reach source material.
     */
    private Set<String> retrievableSourceIds() {
        return licensePolicy.retrievableSourceIds();
    }

    private RetrievalResult search(RetrievalQuery query, Set<String> allowed, long started) {
        EmbeddingModelInfo model = embedder.isAvailable() ? embedder.info() : EmbeddingModelInfo.NONE;
        boolean useVectors = model.isPresent() && index.isVectorCapable(model);

        Query filter = sourceFilter(allowed);
        Query lexical = lexicalQuery(query, filter);
        float[] vector = useVectors ? embedder.embed(query.text()) : null;

        return index.search(searcher -> {
            List<ScoreFusion.Candidate> lexicalHits = run(searcher, lexical);
            List<ScoreFusion.Candidate> vectorHits = vector == null
                    ? List.of()
                    : run(searcher, new KnnFloatVectorQuery(IndexFields.VECTOR, vector, candidates, filter));

            FusionWeights weights = vector == null
                    ? FusionWeights.lexicalOnly(new FusionWeights(lexicalWeight, vectorWeight, conceptBoost, takeawayBoost))
                    : new FusionWeights(lexicalWeight, vectorWeight, conceptBoost, takeawayBoost);

            Map<String, Double> boosts = boosts(searcher, lexicalHits, vectorHits, query);
            List<ScoreFusion.Fused> fused = ScoreFusion.fuse(
                    lexicalHits, vectorHits, weights, boosts, maxPerDocument, query.limit());

            List<RetrievedChunk> chunks = new ArrayList<>(fused.size());
            for (ScoreFusion.Fused hit : fused) {
                Document document = documentFor(searcher, hit.chunkId());
                if (document != null) {
                    chunks.add(toChunk(document, hit));
                }
            }
            return new RetrievalResult(chunks, vector != null, System.currentTimeMillis() - started);
        }, RetrievalResult.EMPTY);
    }

    private Query sourceFilter(Set<String> allowed) {
        return new TermInSetQuery(IndexFields.SOURCE_ID,
                allowed.stream().map(BytesRef::new).toList());
    }

    /**
     * Ordinary words match the prose field; harmonic symbols additionally match the symbol
     * field, boosted, because an exact {@code Ger+6} is a far stronger signal than the
     * words around it.
     */
    Query lexicalQuery(RetrievalQuery query, Query filter) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Analyzer analyzer = MusicAnalyzer.create();

        for (String token : analyse(analyzer, IndexFields.CONTENT, query.text())) {
            builder.add(new TermQuery(new Term(IndexFields.CONTENT, token)), BooleanClause.Occur.SHOULD);
            builder.add(new BoostQuery(
                    new TermQuery(new Term(IndexFields.SECTION_TITLE, token)), 1.5f), BooleanClause.Occur.SHOULD);
            builder.add(new BoostQuery(
                    new TermQuery(new Term(IndexFields.DOCUMENT_TITLE, token)), 1.5f), BooleanClause.Occur.SHOULD);
        }
        for (String symbol : query.symbols()) {
            builder.add(new BoostQuery(
                    new TermQuery(new Term(IndexFields.SYMBOL, symbol)), 3.0f), BooleanClause.Occur.SHOULD);
            for (String part : MusicSymbols.expand(symbol)) {
                builder.add(new BoostQuery(
                        new TermQuery(new Term(IndexFields.SYMBOL, part)), 1.2f), BooleanClause.Occur.SHOULD);
            }
        }
        builder.add(filter, BooleanClause.Occur.FILTER);
        builder.setMinimumNumberShouldMatch(1);
        return builder.build();
    }

    static List<String> analyse(Analyzer analyzer, String field, String text) {
        List<String> tokens = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream(field, text)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(term.toString());
            }
            stream.end();
        } catch (IOException e) {
            LOG.debugf("Could not analyse query text: %s", e.toString());
        }
        return tokens;
    }

    private List<ScoreFusion.Candidate> run(IndexSearcher searcher, Query query) throws IOException {
        TopDocs top = searcher.search(query, candidates);
        List<ScoreFusion.Candidate> hits = new ArrayList<>(top.scoreDocs.length);
        for (ScoreDoc scoreDoc : top.scoreDocs) {
            Document document = searcher.storedFields().document(scoreDoc.doc);
            hits.add(new ScoreFusion.Candidate(
                    document.get(IndexFields.CHUNK_ID),
                    document.get(IndexFields.DOCUMENT_ID),
                    orderOf(document),
                    scoreDoc.score));
        }
        return hits;
    }

    private Map<String, Double> boosts(IndexSearcher searcher,
                                       List<ScoreFusion.Candidate> lexical,
                                       List<ScoreFusion.Candidate> vector,
                                       RetrievalQuery query) {
        Map<String, Double> boosts = new HashMap<>();
        List<ScoreFusion.Candidate> all = new ArrayList<>(lexical);
        all.addAll(vector);
        for (ScoreFusion.Candidate candidate : all) {
            if (boosts.containsKey(candidate.chunkId())) {
                continue;
            }
            Document document = documentFor(searcher, candidate.chunkId());
            if (document == null) {
                continue;
            }
            double boost = 0.0;
            if (query.conceptId() != null) {
                for (String concept : document.getValues(IndexFields.CONCEPT)) {
                    if (query.conceptId().equals(concept)) {
                        boost += conceptBoost;
                        break;
                    }
                }
            }
            if ("TAKEAWAY".equals(document.get(IndexFields.KIND))) {
                boost += takeawayBoost;
            }
            boosts.put(candidate.chunkId(), boost);
        }
        return boosts;
    }

    private Document documentFor(IndexSearcher searcher, String chunkId) {
        try {
            TopDocs hit = searcher.search(new TermQuery(new Term(IndexFields.CHUNK_ID, chunkId)), 1);
            if (hit.scoreDocs.length == 0) {
                return null;
            }
            return searcher.storedFields().document(hit.scoreDocs[0].doc);
        } catch (IOException e) {
            return null;
        }
    }

    private static long orderOf(Document document) {
        String value = document.get(IndexFields.ORDER);
        try {
            return value == null ? 0L : Long.parseLong(value);
        } catch (NumberFormatException notANumber) {
            return 0L;
        }
    }

    private static RetrievedChunk toChunk(Document document, ScoreFusion.Fused hit) {
        return new RetrievedChunk(
                document.get(IndexFields.CHUNK_ID),
                document.get(IndexFields.DOCUMENT_ID),
                document.get(IndexFields.SOURCE_ID),
                document.get(IndexFields.DOCUMENT_TITLE),
                document.get(IndexFields.SECTION_TITLE),
                document.get(IndexFields.CONTENT),
                document.get(IndexFields.ATTRIBUTION),
                document.get(IndexFields.LICENSE_ID),
                document.get(IndexFields.URL),
                hit.score(),
                hit.lexical(),
                hit.vector());
    }
}
