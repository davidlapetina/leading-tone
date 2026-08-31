package fr.lapetina.music.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.learner.EvidenceResult;
import fr.lapetina.music.midi.MidiEvaluator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExerciseEvaluatorTest {

    private ExerciseEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ExerciseEvaluator();
        evaluator.midiEvaluator = new MidiEvaluator();
    }

    @Test
    void readsAnswersWrittenAsSentences() {
        ExpectedAnswer expected = ExpectedAnswer.text("V7", "V7");
        assertEquals(EvidenceResult.CORRECT, evaluator.evaluateText(expected, "I think it's V7", null).result());
        assertEquals(EvidenceResult.CORRECT, evaluator.evaluateText(expected, "  v7  ", null).result());
    }

    @Test
    @DisplayName("Eb does not slip through as an answer of E")
    void doesNotAcceptANearMissAsExact() {
        ExpectedAnswer expected = ExpectedAnswer.text("E", "E");
        assertEquals(EvidenceResult.INCORRECT, evaluator.evaluateText(expected, "Eb", null).result());
    }

    @Test
    void understandsFlatAndSharpWrittenOut() {
        ExpectedAnswer expected = ExpectedAnswer.noteSet(List.of("F", "Ab", "C"));
        assertEquals(EvidenceResult.CORRECT,
                evaluator.evaluateText(expected, "F, A flat and C", null).result());
        assertEquals(EvidenceResult.CORRECT,
                evaluator.evaluateText(expected, "Ab C F", null).result());
    }

    @Test
    @DisplayName("the right sound with the wrong spelling earns partial credit and an explanation")
    void marksSpellingSeparatelyFromSound() {
        ExpectedAnswer expected = ExpectedAnswer.noteSet(List.of("F#", "A#", "C#"));
        EvaluationOutcome outcome = evaluator.evaluateText(expected, "Gb Bb Db", null);
        assertEquals(EvidenceResult.PARTIALLY_CORRECT, outcome.result());
        assertTrue(outcome.feedback().contains("spelling"), outcome.feedback());
    }

    @Test
    void singleNoteEnharmonicsGetPartialCredit() {
        ExpectedAnswer expected = ExpectedAnswer.text("G#", "G#");
        EvaluationOutcome outcome = evaluator.evaluateText(expected, "Ab", null);
        assertEquals(EvidenceResult.PARTIALLY_CORRECT, outcome.result());
        assertTrue(outcome.feedback().contains("spelled G#"), outcome.feedback());
    }

    @Test
    void orderMattersForScalesButNotForChords() {
        ExpectedAnswer scale = ExpectedAnswer.noteSequence(List.of("C", "D", "E", "F", "G", "A", "B"));
        assertEquals(EvidenceResult.INCORRECT, evaluator.evaluateText(scale, "C E D F G A B", null).result());

        ExpectedAnswer chord = ExpectedAnswer.noteSet(List.of("C", "E", "G"));
        assertEquals(EvidenceResult.CORRECT, evaluator.evaluateText(chord, "G C E", null).result());
    }

    @Test
    @DisplayName("\"I don't know\" is recorded as a skip, not as a wrong answer")
    void notKnowingIsNotTheSameAsBeingWrong() {
        EvaluationOutcome outcome = evaluator.evaluateText(
                ExpectedAnswer.text("V7", "V7"), "I don't know", null);
        assertEquals(EvidenceResult.SKIPPED, outcome.result());
    }

    @Test
    void anEmptyAnswerIsSkippedRatherThanWrong() {
        assertEquals(EvidenceResult.SKIPPED,
                evaluator.evaluateText(ExpectedAnswer.text("V7", "V7"), "   ", null).result());
    }

    @Test
    @DisplayName("a free explanation is handed on for judgement, not silently marked")
    void doesNotPretendToGradeProse() {
        EvaluationOutcome outcome = evaluator.evaluateText(
                ExpectedAnswer.explanation("why the leading tone rises"), "because it wants to", null);
        assertTrue(outcome.requiresModelJudgement());
        assertEquals(0.0, outcome.confidence());
    }

    @Test
    @DisplayName("typing the notes of a keyboard exercise is worth less than playing them")
    void typingInsteadOfPlayingIsPartialCredit() {
        ExpectedAnswer expected = ExpectedAnswer.midiChord("G/B", "G major in first inversion");
        EvaluationOutcome outcome = evaluator.evaluateText(expected, "G B D", null);
        assertEquals(EvidenceResult.PARTIALLY_CORRECT, outcome.result());
        assertTrue(outcome.feedback().contains("keyboard"), outcome.feedback());
    }

    @Test
    void parsesKeyContext() {
        assertEquals("D major", ExerciseEvaluator.parseKey("D major").name());
        assertEquals("F# minor", ExerciseEvaluator.parseKey("F# minor").name());
        org.junit.jupiter.api.Assertions.assertNull(ExerciseEvaluator.parseKey(null));
    }
}
