package fr.lapetina.music.learner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The learner model end to end, against a real database: evidence goes in, mastery and
 * the review schedule come out, and the history survives.
 */
@QuarkusTest
class EvidenceServiceTest {

    @Inject
    EvidenceService evidenceService;

    @Inject
    LearnerService learnerService;

    @Transactional
    Learner newLearner() {
        return Learner.create("Test " + UUID.randomUUID());
    }

    @Test
    void recordingEvidenceMovesMasteryAndLeavesATrail() {
        Learner learner = newLearner();
        Evidence first = evidenceService.record(learner,
                EvidenceObservation.of("triad", EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT,
                        0.5, "play an F minor triad"));

        assertEquals(0.0, first.masteryBefore);
        assertTrue(first.masteryAfter > 0);
        assertNotNull(first.id);

        Evidence second = evidenceService.record(learner,
                EvidenceObservation.of("triad", EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT,
                        0.5, "play a B diminished triad"));
        assertEquals(first.masteryAfter, second.masteryBefore, 1e-9);

        List<Evidence> history = evidenceService.history(learner, "triad", 10);
        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("the snapshot covers every concept, including ones never touched")
    void buildsAFullSnapshot() {
        Learner learner = newLearner();
        evidenceService.record(learner, EvidenceObservation.of("note", EvidenceType.TEXT_RECALL,
                EvidenceResult.CORRECT, 0.3, "name a note"));

        LearnerSnapshot snapshot = learnerService.snapshot(learner);
        assertTrue(snapshot.concepts().size() >= 20);
        assertTrue(snapshot.concept("note").mastery() > 0);
        assertEquals(LearningState.UNKNOWN, snapshot.concept("modulation").state());
        assertEquals(1, snapshot.inProgress().size());
    }

    @Test
    void aRunOfCorrectAnswersSchedulesAReview() {
        Learner learner = newLearner();
        for (int i = 0; i < 6; i++) {
            evidenceService.record(learner, EvidenceObservation.of("interval", EvidenceType.MIDI_INTERVAL,
                    EvidenceResult.CORRECT, 0.5, "play a major sixth above D"));
        }
        LearnerConcept concept = LearnerConcept.find("learner = ?1 and conceptId = ?2", learner, "interval")
                .firstResult();
        assertNotNull(concept.nextReviewAt);
        assertTrue(concept.reviewIntervalDays >= 1);
        assertTrue(concept.mastery > 0.45);
    }

    @Test
    @DisplayName("succeeding at the keyboard shifts the learner's profile towards the keyboard")
    void preferencesFollowBehaviour() {
        Learner learner = newLearner();
        double before = learner.preferences.keyboardPreference;
        for (int i = 0; i < 10; i++) {
            evidenceService.record(learner, EvidenceObservation.of("triad", EvidenceType.MIDI_CHORD,
                    EvidenceResult.CORRECT, 0.5, "play a triad"));
        }
        assertTrue(learner.preferences.keyboardPreference > before);
    }

    @Test
    void unknownConceptsAreRejected() {
        Learner learner = newLearner();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> evidenceService.record(learner, EvidenceObservation.of("jazz-in-general",
                        EvidenceType.TEXT_RECALL, EvidenceResult.CORRECT, 0.5, "nope")));
    }
}
