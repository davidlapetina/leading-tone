package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProgressionAnalyzerTest {

    private static ProgressionAnalysis analyze(Key key, String... symbols) {
        return ProgressionAnalyzer.analyze(List.of(symbols).stream().map(ChordAnalyzer::parse).toList(), key);
    }

    @Test
    void labelsDiatonicProgressions() {
        ProgressionAnalysis analysis = analyze(Key.major("C"), "C", "F", "G7", "C");
        assertEquals("I - IV - V7 - I", analysis.romanNumeralLine());
        assertTrue(analysis.allDiatonic());
    }

    @Test
    void labelsInversionsWithFiguredBass() {
        ProgressionAnalysis analysis = analyze(Key.major("C"), "C", "G/B", "Am", "G7");
        assertEquals("I - V6 - vi - V7", analysis.romanNumeralLine());
    }

    @Test
    @DisplayName("a D7 in C major is heard as the dominant of the dominant")
    void findsSecondaryDominants() {
        ProgressionAnalysis analysis = analyze(Key.major("C"), "C", "D7", "G7", "C");
        assertEquals("I - V7/V - V7 - I", analysis.romanNumeralLine());
        assertFalse(analysis.allDiatonic());
        assertEquals(HarmonicFunction.APPLIED_DOMINANT, analysis.chords().get(1).function());
    }

    @Test
    void findsAppliedLeadingToneChords() {
        ProgressionAnalysis analysis = analyze(Key.major("C"), "C#dim7", "Dm");
        assertEquals("vii°7/ii - ii", analysis.romanNumeralLine());
    }

    @Test
    void namesCadences() {
        assertEquals(List.of(Cadence.PERFECT_AUTHENTIC),
                analyze(Key.major("C"), "F", "G", "C").cadences().stream().map(CadencePoint::cadence).toList());
        assertEquals(List.of(Cadence.DECEPTIVE),
                analyze(Key.major("C"), "G", "Am").cadences().stream().map(CadencePoint::cadence).toList());
        assertEquals(List.of(Cadence.PLAGAL),
                analyze(Key.major("C"), "F", "C").cadences().stream().map(CadencePoint::cadence).toList());
        assertEquals(List.of(Cadence.HALF),
                analyze(Key.major("C"), "C", "G").cadences().stream().map(CadencePoint::cadence).toList());
    }

    @Test
    void analysesMinorKeys() {
        ProgressionAnalysis analysis = analyze(Key.minor("A"), "Am", "Dm", "E7", "Am");
        assertEquals("i - iv - V7 - i", analysis.romanNumeralLine());
    }

    @Test
    void marksUnexplainedChordsRatherThanGuessing() {
        ProgressionAnalysis analysis = analyze(Key.major("C"), "C", "Ebm");
        assertEquals("I - ?", analysis.romanNumeralLine());
        assertEquals(HarmonicFunction.CHROMATIC, analysis.chords().get(1).function());
    }

    @Test
    void summarisesForTheTutor() {
        String summary = analyze(Key.major("C"), "C", "F", "G7", "C").summary();
        assertTrue(summary.contains("C major"));
        assertTrue(summary.contains("I - IV - V7 - I"));
        assertTrue(summary.contains("perfect authentic cadence"));
    }
}
