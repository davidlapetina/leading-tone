package fr.lapetina.music.midi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.learner.EvidenceResult;
import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.ChordQuality;
import fr.lapetina.music.theory.Inversion;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MidiEvaluatorTest {

    private final MidiEvaluator evaluator = new MidiEvaluator();

    private static MidiPerformance played(Integer... notes) {
        return MidiPerformance.of(List.of(notes));
    }

    @Test
    void acceptsTheRightChordInTheRightInversion() {
        Chord expected = Chord.of("G", ChordQuality.MAJOR, Inversion.FIRST);
        MidiEvaluation evaluation = evaluator.evaluateChord(expected, played(59, 62, 67), null);
        assertEquals(EvidenceResult.CORRECT, evaluation.result());
        assertTrue(evaluation.correctBass());
    }

    @Test
    @DisplayName("root position instead of an inversion is a named mistake, not just wrong")
    void separatesTheChordFromItsBass() {
        Chord expected = Chord.of("G", ChordQuality.MAJOR, Inversion.FIRST);
        MidiEvaluation evaluation = evaluator.evaluateChord(expected, played(55, 59, 62), null);

        assertEquals(EvidenceResult.PARTIALLY_CORRECT, evaluation.result());
        assertTrue(evaluation.correctPitchClasses());
        assertFalse(evaluation.correctBass());
        assertEquals(MidiEvaluator.ROOT_POSITION_DEFAULT, evaluation.misconceptionCode());
        assertTrue(evaluation.feedback().contains("bass should be B"), evaluation.feedback());
    }

    @Test
    void spacingAndDoublingDoNotMatter() {
        Chord expected = Chord.of("G", ChordQuality.MAJOR, Inversion.FIRST);
        assertEquals(EvidenceResult.CORRECT, evaluator.evaluateChord(expected, played(59, 67, 74, 79), null).result());
    }

    @Test
    @DisplayName("leaving out the seventh is recognised as leaving out the seventh")
    void noticesAMissingSeventh() {
        Chord expected = ChordAnalyzer.parse("G7");
        MidiEvaluation evaluation = evaluator.evaluateChord(expected, played(55, 59, 62), null);
        assertEquals(EvidenceResult.INCORRECT, evaluation.result());
        assertEquals(MidiEvaluator.OMITS_SEVENTH, evaluation.misconceptionCode());
        assertEquals(List.of("F"), evaluation.missing());
    }

    @Test
    void noticesAWrongThird() {
        Chord expected = Chord.of("D", ChordQuality.MAJOR);
        MidiEvaluation evaluation = evaluator.evaluateChord(expected, played(50, 53, 57), null);
        assertEquals(EvidenceResult.INCORRECT, evaluation.result());
        assertEquals(MidiEvaluator.WRONG_CHORD_QUALITY, evaluation.misconceptionCode());
        assertEquals("Dm", evaluation.detected());
    }

    @Test
    void reportsWhatWasMissingAndWhatWasExtra() {
        Chord expected = Chord.of("C", ChordQuality.MAJOR);
        MidiEvaluation evaluation = evaluator.evaluateChord(expected, played(60, 65, 67), null);
        assertEquals(List.of("E"), evaluation.missing());
        assertEquals(List.of("F"), evaluation.extra());
    }

    @Test
    void silenceIsRecordedAsSkipped() {
        MidiEvaluation evaluation = evaluator.evaluateChord(Chord.of("C", ChordQuality.MAJOR),
                MidiPerformance.of(List.of()), null);
        assertEquals(EvidenceResult.SKIPPED, evaluation.result());
    }

    @Test
    void acceptsACorrectScale() {
        Scale scale = Scale.of("D", ScaleType.MAJOR);
        MidiEvaluation evaluation = evaluator.evaluateScale(scale,
                played(62, 64, 66, 67, 69, 71, 73, 74));
        assertEquals(EvidenceResult.CORRECT, evaluation.result());
    }

    @Test
    @DisplayName("a scale stopped at the seventh still counts")
    void doesNotInsistOnTheClosingTonic() {
        Scale scale = Scale.of("D", ScaleType.MAJOR);
        assertEquals(EvidenceResult.CORRECT,
                evaluator.evaluateScale(scale, played(62, 64, 66, 67, 69, 71, 73)).result());
    }

    @Test
    void saysWhichDegreeWentWrong() {
        Scale scale = Scale.of("D", ScaleType.MAJOR);
        MidiEvaluation evaluation = evaluator.evaluateScale(scale, played(62, 64, 65, 67, 69, 71, 73, 74));
        assertEquals(EvidenceResult.INCORRECT, evaluation.result());
        assertTrue(evaluation.feedback().contains("Degree 3"), evaluation.feedback());
        assertTrue(evaluation.feedback().contains("F#"), evaluation.feedback());
    }

    @Test
    @DisplayName("a natural seventh in harmonic minor is a specific, nameable mistake")
    void noticesAnUnraisedLeadingTone() {
        Scale scale = Scale.of("A", ScaleType.HARMONIC_MINOR);
        MidiEvaluation evaluation = evaluator.evaluateScale(scale, played(69, 71, 72, 74, 76, 77, 79, 81));
        assertEquals(EvidenceResult.INCORRECT, evaluation.result());
        assertEquals(MidiEvaluator.UNRAISED_LEADING_TONE, evaluation.misconceptionCode());
    }

    @Test
    void checksPlainNoteSets() {
        MidiEvaluation evaluation = evaluator.evaluateNotes(
                List.of(fr.lapetina.music.theory.Note.parse("D4"), fr.lapetina.music.theory.Note.parse("B4")),
                played(62, 71), Key.major("D"));
        assertEquals(EvidenceResult.CORRECT, evaluation.result());
    }
}
