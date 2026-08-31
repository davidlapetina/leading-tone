package fr.lapetina.music.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.chunk.ChunkKind;
import fr.lapetina.music.knowledge.chunk.KnowledgeChunk;
import fr.lapetina.music.knowledge.chunk.KnowledgeDocument;
import fr.lapetina.music.knowledge.embedding.EmbeddingService;
import fr.lapetina.music.knowledge.index.IndexBuilder;
import fr.lapetina.music.knowledge.index.KnowledgeIndex;
import fr.lapetina.music.knowledge.index.KnowledgePaths;
import fr.lapetina.music.knowledge.retrieval.KnowledgeRetriever;
import fr.lapetina.music.knowledge.retrieval.RetrievalQuery;
import fr.lapetina.music.knowledge.source.IngestionMode;
import fr.lapetina.music.knowledge.source.KnowledgeSource;
import fr.lapetina.music.knowledge.source.SourceState;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Searching for the symbols this subject is written in, through the real index.
 *
 * <p>Analyzer unit tests prove the tokens come out right; this proves the query built from
 * a learner's words actually finds the passage. They fail differently — a correct analyzer
 * paired with a query that re-analyses its input a second way finds nothing — so both are
 * worth having.
 */
@QuarkusTest
class SymbolSearchTest {

    private static final String SOURCE = "open-music-theory";

    /** One passage per symbol, so a hit is unambiguous. */
    private static final Map<String, String> PASSAGES = new LinkedHashMap<>();

    static {
        PASSAGES.put("V/V", "The applied dominant V/V tonicizes the dominant without a seventh.");
        PASSAGES.put("V7/V", "Adding the seventh gives V7/V, which in C major is D F# A C.");
        PASSAGES.put("iiø7", "The half-diminished supertonic iiø7 is the usual predominant in minor.");
        PASSAGES.put("vii°7", "A fully diminished vii°7 stacks three minor thirds above the leading tone.");
        PASSAGES.put("Ger+6", "The German augmented sixth Ger+6 has a perfect fifth above its bass.");
        PASSAGES.put("Fr+6", "The French augmented sixth Fr+6 contains an augmented fourth.");
        PASSAGES.put("It+6", "The Italian augmented sixth It+6 has only three notes and doubles the third.");
        PASSAGES.put("N6", "The Neapolitan N6 is a flat-two major triad in first inversion.");
        PASSAGES.put("bII", "Written bII when it appears in root position rather than as a sixth chord.");
        PASSAGES.put("#iv°", "A raised subdominant diminished triad #iv° often precedes the dominant.");
        PASSAGES.put("ii-V-I", "The ii-V-I progression is the backbone of traditional jazz harmony.");
    }

    @Inject
    IndexBuilder indexBuilder;

    @Inject
    KnowledgeIndex index;

    @Inject
    KnowledgeRetriever retriever;

    @Inject
    KnowledgePaths paths;

    @Inject
    EmbeddingService embedder;

    @BeforeEach
    void indexOnePassagePerSymbol() {
        KnowledgePaths.deleteRecursively(paths.root());
        indexBuilder.build(1, writeChunks(), embedder);
        paths.writeCurrent(1);
        index.activate(1);
        activateSource();
    }

    @Transactional
    List<KnowledgeChunk> writeChunks() {
        KnowledgeChunk.deleteAll();
        KnowledgeDocument.deleteAll();
        UUID documentId = UUID.randomUUID();

        KnowledgeDocument document = new KnowledgeDocument();
        document.id = documentId;
        document.sourceId = SOURCE;
        document.generation = 1;
        document.externalId = "symbols";
        document.title = "Chromatic Harmony";
        document.licenseId = "CC-BY-SA-4.0";
        document.attribution = "Open Music Theory. CC BY-SA 4.0.";
        document.checksum = "test";
        document.active = true;
        document.ingestedAt = Instant.now();
        document.persist();

        int order = 0;
        for (Map.Entry<String, String> passage : PASSAGES.entrySet()) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.documentId = documentId;
            chunk.sourceId = SOURCE;
            chunk.generation = 1;
            chunk.chunkKey = "symbol-" + order;
            chunk.documentTitle = "Chromatic Harmony";
            chunk.sectionTitle = passage.getKey();
            chunk.sectionOrder = order++;
            chunk.chunkOrder = 0;
            chunk.kind = ChunkKind.PROSE;
            chunk.body = passage.getValue();
            chunk.licenseId = "CC-BY-SA-4.0";
            chunk.attribution = "Open Music Theory. CC BY-SA 4.0.";
            chunk.wordCount = passage.getValue().split("\\s+").length;
            chunk.active = true;
            chunk.persist();
        }
        return KnowledgeChunk.listAll();
    }

    @Transactional
    void activateSource() {
        KnowledgeSource source = KnowledgeSource.byId(SOURCE);
        if (source == null) {
            source = new KnowledgeSource();
            source.id = SOURCE;
            source.displayName = "Open Music Theory";
            source.ingestionMode = IngestionMode.TEXT_RAG;
        }
        source.licenseId = "CC-BY-SA-4.0";
        source.state = SourceState.ACTIVE;
        source.enabled = true;
        source.updatedAt = Instant.now();
        source.persist();
    }

    private String topHitFor(String query) {
        var chunks = retriever.retrieve(new RetrievalQuery(query, null, 3)).chunks();
        return chunks.isEmpty() ? null : chunks.get(0).sectionTitle();
    }

    @Test
    @DisplayName("every symbol in the subject's vocabulary finds its own passage")
    void findsEachSymbol() {
        // The per-document cap allows two, so a whole-vocabulary sweep is the honest check:
        // each symbol must rank its own passage first, not merely appear somewhere.
        for (String symbol : PASSAGES.keySet()) {
            assertEquals(symbol, topHitFor(symbol),
                    "searching for " + symbol + " should find the passage about " + symbol);
        }
    }

    @Test
    @DisplayName("an applied dominant is not the same search as a plain one")
    void tellsApartSymbolsThatSharePrefixes() {
        assertEquals("V7/V", topHitFor("V7/V"));
        assertEquals("V/V", topHitFor("V/V"));
        assertEquals("N6", topHitFor("N6"));
        assertEquals("bII", topHitFor("bII"));
    }

    @Test
    @DisplayName("half-diminished and fully diminished do not collapse into each other")
    void tellsApartDiminishedSigns() {
        assertEquals("iiø7", topHitFor("iiø7"));
        assertEquals("vii°7", topHitFor("vii°7"));
    }

    @Test
    @DisplayName("the three augmented sixths are three different chords")
    void tellsApartTheAugmentedSixths() {
        assertEquals("Ger+6", topHitFor("Ger+6"));
        assertEquals("Fr+6", topHitFor("Fr+6"));
        assertEquals("It+6", topHitFor("It+6"));
    }

    @Test
    @DisplayName("a question written as a sentence still finds the symbol inside it")
    void findsSymbolsInsideOrdinaryQuestions() {
        assertEquals("Ger+6", topHitFor("why does Ger+6 resolve to the dominant"));
        assertEquals("ii-V-I", topHitFor("what is a ii-V-I in jazz"));
    }

    @Test
    @DisplayName("plain words still work; this is a hybrid, not a symbol index")
    void stillMatchesOrdinaryLanguage() {
        assertFalse(retriever.retrieve(RetrievalQuery.of("jazz harmony backbone")).isEmpty());
        assertTrue(retriever.retrieve(RetrievalQuery.of("leading tone")).chunks().stream()
                .anyMatch(chunk -> chunk.body().contains("leading tone")));
    }
}
