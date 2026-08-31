package fr.lapetina.music.learner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MasteryServiceTest {

    private final MasteryService masteryService = new MasteryService();
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    private LearnerConcept fresh() {
        LearnerConcept concept = new LearnerConcept();
        concept.conceptId = "dominant-seventh";
        return concept;
    }

    private void answer(LearnerConcept concept, EvidenceType type, EvidenceResult result, int times) {
        for (int i = 0; i < times; i++) {
            masteryService.apply(concept, type, result, 0.6, 1.0, now);
        }
    }

    @Test
    void weightsEvidenceByKindDifficultyAndConfidence() {
        assertEquals(0.64, masteryService.weightOf(EvidenceType.MIDI_CHORD, 0.6, 1.0), 1e-9);
        assertEquals(0.32, masteryService.weightOf(EvidenceType.MULTIPLE_CHOICE, 0.6, 1.0), 1e-9);
        assertTrue(masteryService.weightOf(EvidenceType.EXPLANATION, 0.6, 1.0)
                > masteryService.weightOf(EvidenceType.TEXT_RECALL, 0.6, 1.0));
        assertTrue(masteryService.weightOf(EvidenceType.MIDI_CHORD, 0.9, 1.0)
                > masteryService.weightOf(EvidenceType.MIDI_CHORD, 0.2, 1.0));
    }

    @Test
    @DisplayName("no single answer can carry a concept very far")
    void gainsAreAsymptotic() {
        LearnerConcept concept = fresh();
        MasteryUpdate update = masteryService.apply(concept, EvidenceType.MIDI_CHORD,
                EvidenceResult.CORRECT, 0.6, 1.0, now);
        assertEquals(0.192, update.masteryAfter(), 1e-9);
        assertEquals(LearningState.LEARNING, concept.state);
        assertEquals(0.25, concept.confidence, 1e-9);
    }

    @Test
    void repeatedStrongEvidenceReachesMastery() {
        LearnerConcept concept = fresh();
        answer(concept, EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT, 9);
        assertEquals(LearningState.RELIABLE, concept.state);

        answer(concept, EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT, 1);
        assertTrue(concept.mastery > 0.88);
        assertEquals(LearningState.MASTERED, concept.state);
    }

    @Test
    @DisplayName("multiple choice alone never earns MASTERED, however high the number goes")
    void weakEvidenceCannotClaimMastery() {
        LearnerConcept concept = fresh();
        answer(concept, EvidenceType.MULTIPLE_CHOICE, EvidenceResult.CORRECT, 40);
        assertTrue(concept.mastery > 0.9, "mastery should be high: " + concept.mastery);
        assertTrue(concept.confidence > 0.9);
        assertEquals(0, concept.strongEvidence);
        assertEquals(LearningState.RELIABLE, concept.state);
        assertNotEquals(LearningState.MASTERED, concept.state);
    }

    @Test
    void mistakesCostMoreWhenMasteryWasHigh() {
        LearnerConcept high = fresh();
        answer(high, EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT, 5);
        double beforeHigh = high.mastery;
        answer(high, EvidenceType.MIDI_CHORD, EvidenceResult.INCORRECT, 1);
        double dropFromHigh = beforeHigh - high.mastery;

        LearnerConcept low = fresh();
        answer(low, EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT, 1);
        double beforeLow = low.mastery;
        answer(low, EvidenceType.MIDI_CHORD, EvidenceResult.INCORRECT, 1);
        double dropFromLow = beforeLow - low.mastery;

        assertTrue(dropFromHigh > dropFromLow);
    }

    @Test
    void skippingCostsLittle() {
        LearnerConcept skipped = fresh();
        answer(skipped, EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT, 4);
        double before = skipped.mastery;
        answer(skipped, EvidenceType.MIDI_CHORD, EvidenceResult.SKIPPED, 1);
        double afterSkip = skipped.mastery;

        LearnerConcept wrong = fresh();
        answer(wrong, EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT, 4);
        answer(wrong, EvidenceType.MIDI_CHORD, EvidenceResult.INCORRECT, 1);

        assertTrue(afterSkip > wrong.mastery);
        assertTrue(afterSkip < before);
    }

    @Test
    void partialCreditMovesLessThanFullCredit() {
        LearnerConcept partial = fresh();
        answer(partial, EvidenceType.MIDI_CHORD, EvidenceResult.PARTIALLY_CORRECT, 1);
        LearnerConcept full = fresh();
        answer(full, EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT, 1);
        assertTrue(partial.mastery < full.mastery);
        assertTrue(partial.mastery > 0);
    }

    @Test
    void aDueConceptIsMarkedForReviewWhateverTheNumberSays() {
        LearnerConcept concept = fresh();
        answer(concept, EvidenceType.MIDI_CHORD, EvidenceResult.CORRECT, 10);
        concept.nextReviewAt = now.minusSeconds(3600);
        assertEquals(LearningState.NEEDS_REVIEW, masteryService.deriveState(concept, now));
    }

    @Test
    void unseenConceptsStayUnknown() {
        assertEquals(LearningState.UNKNOWN, masteryService.deriveState(fresh(), now));
    }
}
