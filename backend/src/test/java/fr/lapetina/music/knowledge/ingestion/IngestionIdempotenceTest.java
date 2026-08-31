package fr.lapetina.music.knowledge.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Re-running an ingestion must be a no-op when nothing has changed, and must rebuild when
 * anything that affects the result has.
 *
 * <p>The fingerprint is what decides. Everything capable of changing the output belongs in
 * it: the content, the parser that read it, the policy that cut it up, the analyzer that
 * indexed it and the model that embedded it. Leaving the model out is the subtle version of
 * this bug — the index silently keeps vectors from a model that is no longer in use.
 */
class IngestionIdempotenceTest {

    private static final List<String> CONTENT = List.of("a1b2", "c3d4");

    private static String fingerprint(String version, List<String> content, int parser,
                                      int chunkPolicy, int analyzer, String embedding) {
        return Checksums.fingerprint(version, content, parser, chunkPolicy, analyzer, embedding);
    }

    private static String baseline() {
        return fingerprint("2", CONTENT, 1, 1, 1, "bge-small-en-v1.5-q/1.5/384");
    }

    @Test
    @DisplayName("the same source read the same way fingerprints the same, so nothing is redone")
    void isStableWhenNothingChanged() {
        assertEquals(baseline(), baseline());
        assertEquals(baseline(), fingerprint("2", List.of("c3d4", "a1b2"), 1, 1, 1,
                "bge-small-en-v1.5-q/1.5/384"), "document order is not a change");
    }

    @Test
    void changesWhenTheContentChanges() {
        assertNotEquals(baseline(), fingerprint("2", List.of("a1b2", "different"), 1, 1, 1,
                "bge-small-en-v1.5-q/1.5/384"));
        assertNotEquals(baseline(), fingerprint("3", CONTENT, 1, 1, 1,
                "bge-small-en-v1.5-q/1.5/384"), "a new upstream version is a change");
    }

    @Test
    @DisplayName("changing how it is read counts as a change, even when the source has not moved")
    void changesWhenTheProcessingChanges() {
        assertNotEquals(baseline(), fingerprint("2", CONTENT, 2, 1, 1, "bge-small-en-v1.5-q/1.5/384"),
                "a new parser may read the same bytes differently");
        assertNotEquals(baseline(), fingerprint("2", CONTENT, 1, 2, 1, "bge-small-en-v1.5-q/1.5/384"),
                "a new chunk policy cuts the same text into different passages");
        assertNotEquals(baseline(), fingerprint("2", CONTENT, 1, 1, 2, "bge-small-en-v1.5-q/1.5/384"),
                "a new analyzer indexes the same passages under different terms");
    }

    @Test
    @DisplayName("changing the embedding model forces a rebuild rather than mixing vectors")
    void changesWhenTheEmbeddingModelChanges() {
        assertNotEquals(baseline(), fingerprint("2", CONTENT, 1, 1, 1, "all-minilm-l6-v2/1.0/384"));
        assertNotEquals(baseline(), fingerprint("2", CONTENT, 1, 1, 1, "none/0/0"));
    }

    @Test
    void checksumsTheSameTextTheSameWay() {
        assertEquals(Checksums.of("secondary dominant"), Checksums.of("secondary dominant"));
        assertNotEquals(Checksums.of("secondary dominant"), Checksums.of("Secondary dominant"));
    }
}
