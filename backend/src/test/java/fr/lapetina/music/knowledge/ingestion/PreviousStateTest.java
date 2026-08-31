package fr.lapetina.music.knowledge.ingestion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.source.KnowledgeSource;
import fr.lapetina.music.knowledge.source.SourceState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The unchanged-source check, and the mistake that silently disabled it.
 *
 * <p>The pipeline advances a source through its states as it runs, mutating the row. Holding
 * the entity and asking it afterwards what state the source <em>was</em> in answers with the
 * state this run just set — so the check compared ACTIVE against DOWNLOADED, never matched,
 * and every re-ingest re-embedded the whole source while reporting success.
 *
 * <p>It cost a minute per run and corrupted nothing, which is why it survived: the only
 * visible symptom was that re-ingestion was slow.
 */
class PreviousStateTest {

    private static KnowledgeSource active(String fingerprint) {
        KnowledgeSource source = new KnowledgeSource();
        source.id = "open-music-theory";
        source.state = SourceState.ACTIVE;
        source.fingerprint = fingerprint;
        source.activeGeneration = 3;
        source.documentCount = 124;
        source.chunkCount = 1229;
        return source;
    }

    @Test
    @DisplayName("a snapshot of the source survives the pipeline mutating the row")
    void isNotAffectedByLaterStateChanges() {
        KnowledgeSource source = active("abc123");
        IngestionService.Previous previous = IngestionService.Previous.of(source);

        // What the pipeline does as it advances.
        source.state = SourceState.LICENSE_VERIFIED;
        source.state = SourceState.DOWNLOADED;

        assertTrue(previous.isUnchanged("abc123"),
                "the snapshot must still say the source was ACTIVE before this run began");
    }

    @Test
    void rebuildsWhenTheFingerprintDiffers() {
        assertFalse(IngestionService.Previous.of(active("abc123")).isUnchanged("different"));
    }

    @Test
    @DisplayName("a source that never finished is rebuilt, whatever its fingerprint says")
    void rebuildsWhatWasNeverActive() {
        KnowledgeSource halfDone = active("abc123");
        halfDone.state = SourceState.FAILED;

        assertFalse(IngestionService.Previous.of(halfDone).isUnchanged("abc123"));
    }

    @Test
    void treatsAnUnknownSourceAsNew() {
        IngestionService.Previous none = IngestionService.Previous.of(null);

        assertFalse(none.isUnchanged("abc123"));
        assertFalse(none.isUnchanged(null));
        assertTrue(none.generation() == 0);
    }
}
