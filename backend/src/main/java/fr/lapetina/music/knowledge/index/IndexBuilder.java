package fr.lapetina.music.knowledge.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.knowledge.chunk.KnowledgeChunk;
import fr.lapetina.music.knowledge.embedding.EmbeddingModelInfo;
import fr.lapetina.music.knowledge.embedding.EmbeddingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.jboss.logging.Logger;

/**
 * Writes one complete index generation, then leaves it alone.
 *
 * <p>A generation is built into its own directory while the previous one keeps serving
 * searches. Nothing is activated until the whole build has finished, so an ingestion that
 * fails halfway leaves the running application exactly as it was.
 */
@ApplicationScoped
public class IndexBuilder {

    private static final Logger LOG = Logger.getLogger(IndexBuilder.class);

    @Inject
    KnowledgePaths paths;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Builds a generation from chunks. Vectors are written only when the embedder is
     * available; without one the generation is lexical-only, which is a working index
     * rather than a failure.
     */
    public IndexMeta build(int generation, List<KnowledgeChunk> chunks, EmbeddingService embedder) {
        Path target = paths.generation(generation);
        KnowledgePaths.deleteRecursively(target);
        paths.ensure(target);

        EmbeddingModelInfo model = embedder != null && embedder.isAvailable()
                ? embedder.info()
                : EmbeddingModelInfo.NONE;

        long documents = chunks.stream().map(chunk -> chunk.documentId).distinct().count();
        try (Directory directory = MMapDirectory.open(target);
                IndexWriter writer = new IndexWriter(directory, writerConfig())) {
            for (KnowledgeChunk chunk : chunks) {
                writer.addDocument(toDocument(chunk, model, embedder));
            }
            writer.commit();
        } catch (IOException e) {
            KnowledgePaths.deleteRecursively(target);
            throw new UncheckedIOException("Could not build index generation " + generation, e);
        }

        IndexMeta meta = new IndexMeta(
                generation,
                model.name(),
                model.version(),
                model.dimension(),
                MusicAnalyzer.ANALYZER_VERSION,
                fr.lapetina.music.knowledge.text.ChunkPolicy.CURRENT_VERSION,
                org.apache.lucene.util.Version.LATEST.toString(),
                Instant.now(),
                documents,
                chunks.size());
        writeMeta(target, meta);
        LOG.infof("Built index generation %d: %d chunks from %d documents, embedding %s",
                generation, chunks.size(), documents,
                model.isPresent() ? model.signature() : "none");
        return meta;
    }

    private IndexWriterConfig writerConfig() {
        IndexWriterConfig config = new IndexWriterConfig(MusicAnalyzer.create());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        // The codec is deliberately left at the default. Naming one pins the index to a
        // class that must still resolve by name at read time, which is the usual way a
        // Lucene upgrade turns an existing index into an unreadable one.
        return config;
    }

    private Document toDocument(KnowledgeChunk chunk, EmbeddingModelInfo model, EmbeddingService embedder) {
        Document document = new Document();
        document.add(new StringField(IndexFields.CHUNK_ID, chunk.id.toString(), Field.Store.YES));
        document.add(new StringField(IndexFields.DOCUMENT_ID, chunk.documentId.toString(), Field.Store.YES));
        document.add(new StringField(IndexFields.SOURCE_ID, chunk.sourceId, Field.Store.YES));
        document.add(new StringField(IndexFields.LICENSE_ID, chunk.licenseId, Field.Store.YES));
        document.add(new StringField(IndexFields.KIND, chunk.kind.name(), Field.Store.YES));

        add(document, IndexFields.DOCUMENT_TITLE, chunk.documentTitle);
        add(document, IndexFields.SECTION_TITLE, chunk.sectionTitle);
        document.add(new TextField(IndexFields.CONTENT, chunk.body, Field.Store.YES));
        // The same text again, analysed so that V7/V stays one term. Costs a little disk
        // and is the difference between finding an applied dominant and finding every page
        // that mentions a roman numeral.
        document.add(new TextField(IndexFields.SYMBOL, chunk.body, Field.Store.NO));

        if (chunk.conceptIds != null) {
            for (String concept : chunk.conceptIds.split(",")) {
                if (!concept.isBlank()) {
                    document.add(new StringField(IndexFields.CONCEPT, concept.trim(), Field.Store.YES));
                }
            }
        }
        document.add(new StoredField(IndexFields.ATTRIBUTION, chunk.attribution));
        if (chunk.url != null) {
            document.add(new StoredField(IndexFields.URL, chunk.url));
        }
        long order = (long) chunk.sectionOrder * 1000 + chunk.chunkOrder;
        document.add(new NumericDocValuesField(IndexFields.ORDER, order));
        document.add(new StoredField(IndexFields.ORDER, order));
        document.add(new StoredField(IndexFields.WORD_COUNT, chunk.wordCount));

        if (model.isPresent() && embedder != null) {
            float[] vector = embedder.embed(embeddingText(chunk));
            document.add(new KnnFloatVectorField(
                    IndexFields.VECTOR, vector, VectorSimilarityFunction.DOT_PRODUCT));
        }
        return document;
    }

    /** The title travels with the body so a passage embeds in the context it was written in. */
    private String embeddingText(KnowledgeChunk chunk) {
        StringBuilder text = new StringBuilder();
        if (chunk.documentTitle != null) {
            text.append(chunk.documentTitle).append(". ");
        }
        if (chunk.sectionTitle != null && !chunk.sectionTitle.isBlank()) {
            text.append(chunk.sectionTitle).append(". ");
        }
        return text.append(chunk.body).toString();
    }

    private static void add(Document document, String field, String value) {
        if (value != null && !value.isBlank()) {
            document.add(new TextField(field, value, Field.Store.YES));
        }
    }

    private void writeMeta(Path target, IndexMeta meta) {
        try {
            Files.writeString(target.resolve("index-meta.json"),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write index metadata", e);
        }
    }
}
