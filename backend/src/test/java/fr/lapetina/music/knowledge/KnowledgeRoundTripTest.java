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
import fr.lapetina.music.knowledge.retrieval.RetrievalResult;
import fr.lapetina.music.knowledge.retrieval.RetrievedChunk;
import fr.lapetina.music.knowledge.source.IngestionMode;
import fr.lapetina.music.knowledge.source.KnowledgeSource;
import fr.lapetina.music.knowledge.source.SourceState;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Chunks in, search results out, through the real index.
 *
 * <p>The test profile switches the embedding model off, so this exercises the lexical-only
 * path deliberately: that is the configuration a machine with no model files runs in, and
 * it has to work on its own rather than being a degraded mode nobody tries.
 */
@QuarkusTest
class KnowledgeRoundTripTest {

    private static final String SOURCE = "open-music-theory";

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
    void indexSomePassages() {
        KnowledgePaths.deleteRecursively(paths.root());
        List<KnowledgeChunk> chunks = writeChunks();
        indexBuilder.build(1, chunks, embedder);
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
        document.externalId = "2629";
        document.title = "ii-V-I";
        document.licenseId = "CC-BY-SA-4.0";
        document.attribution = "\"ii-V-I\", Megan Lavengood, Open Music Theory Version 2. CC BY-SA 4.0.";
        document.checksum = "test";
        document.active = true;
        document.ingestedAt = Instant.now();
        document.persist();

        chunk(documentId, 0, "Secondary Dominants",
                "A secondary dominant such as V7/V tonicizes a chord other than the tonic. "
                        + "In C major, V7/V is built on D and contains the note F#, which is not in the key.",
                "secondary-dominant", ChunkKind.PROSE);
        chunk(documentId, 1, "Augmented Sixth Chords",
                "The German augmented sixth, written Ger+6, resolves outward to the dominant. "
                        + "It is spelled with an augmented sixth above the bass.",
                "modal-interchange", ChunkKind.PROSE);
        chunk(documentId, 2, "Key Takeaways",
                "Key Takeaways. The ii-V-I progression is fundamental to jazz harmony.",
                "two-five-one", ChunkKind.TAKEAWAY);
        return KnowledgeChunk.listAll();
    }

    private void chunk(UUID documentId, int order, String section, String body, String concept, ChunkKind kind) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.documentId = documentId;
        chunk.sourceId = SOURCE;
        chunk.generation = 1;
        chunk.chunkKey = "key-" + order;
        chunk.documentTitle = "ii-V-I";
        chunk.sectionTitle = section;
        chunk.sectionOrder = order;
        chunk.chunkOrder = 0;
        chunk.kind = kind;
        chunk.body = body;
        chunk.conceptIds = concept;
        chunk.licenseId = "CC-BY-SA-4.0";
        chunk.attribution = "\"ii-V-I\", Megan Lavengood, Open Music Theory Version 2. CC BY-SA 4.0.";
        chunk.url = "https://viva.pressbooks.pub/openmusictheory/chapter/ii-v-i/";
        chunk.wordCount = body.split("\\s+").length;
        chunk.active = true;
        chunk.persist();
    }

    @Transactional
    void activateSource() {
        KnowledgeSource source = KnowledgeSource.byId(SOURCE);
        if (source == null) {
            source = new KnowledgeSource();
            source.id = SOURCE;
            source.displayName = "Open Music Theory";
            source.licenseId = "CC-BY-SA-4.0";
            source.ingestionMode = IngestionMode.TEXT_RAG;
        }
        source.state = SourceState.ACTIVE;
        source.enabled = true;
        source.activeGeneration = 1;
        source.updatedAt = Instant.now();
        source.persist();
    }

    @Test
    @DisplayName("an ordinary question finds the passage that answers it")
    void findsExplanatoryText() {
        RetrievalResult result = retriever.retrieve(new RetrievalQuery("what is a secondary dominant", null, 4));

        assertFalse(result.isEmpty());
        assertTrue(result.chunks().get(0).body().contains("secondary dominant"));
    }

    @Test
    @DisplayName("an exact symbol finds its own chapter, which is what plain word search cannot do")
    void findsBySymbol() {
        RetrievedChunk top = retriever.retrieve(new RetrievalQuery("Ger+6", null, 4)).chunks().get(0);

        assertTrue(top.body().contains("German augmented sixth"), top.body());
    }

    @Test
    void findsAnAppliedDominantBySymbol() {
        RetrievedChunk top = retriever.retrieve(new RetrievalQuery("V7/V", null, 4)).chunks().get(0);

        assertTrue(top.body().contains("V7/V"), top.body());
    }

    @Test
    @DisplayName("every result can say where it came from and on whose terms")
    void everyResultCarriesItsProvenance() {
        for (RetrievedChunk chunk : retriever.retrieve(RetrievalQuery.of("dominant")).chunks()) {
            assertEquals("CC-BY-SA-4.0", chunk.licenseId());
            assertTrue(chunk.attribution().contains("Megan Lavengood"));
            assertTrue(chunk.attribution().contains("CC BY-SA 4.0"));
            assertEquals(SOURCE, chunk.sourceId());
        }
    }

    @Test
    @DisplayName("with the embedding model off, search still works on words alone")
    void worksWithoutAnEmbeddingModel() {
        RetrievalResult result = retriever.retrieve(RetrievalQuery.of("jazz harmony progression"));

        assertFalse(embedder.isAvailable(), "the test profile runs with no embedding model");
        assertFalse(result.vectorUsed());
        assertFalse(result.isEmpty(), "lexical search has to stand on its own");
    }

    @Test
    @DisplayName("a source that has not finished ingesting is not searchable")
    void doesNotServeAnInactiveSource() {
        deactivateSource();
        try {
            assertTrue(retriever.retrieve(RetrievalQuery.of("secondary dominant")).isEmpty());
        } finally {
            activateSource();
        }
    }

    @Transactional
    void deactivateSource() {
        KnowledgeSource source = KnowledgeSource.byId(SOURCE);
        source.state = SourceState.PARSED;
        source.persist();
    }

    @Test
    void returnsNothingRatherThanFailingOnANonsenseQuery() {
        assertTrue(retriever.retrieve(RetrievalQuery.of("")).isEmpty());
        assertTrue(retriever.retrieve(RetrievalQuery.of("   ")).isEmpty());
    }
}
