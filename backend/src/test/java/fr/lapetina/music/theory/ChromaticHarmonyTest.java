package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChromaticHarmonyTest {

    private static List<String> names(List<PitchClass> pitchClasses) {
        return pitchClasses.stream().map(PitchClass::name).toList();
    }

    private static List<String> chord(String numeral, Key key) {
        return names(RomanNumeralAnalyzer.realize(numeral, key).pitchClasses());
    }

    private static ProgressionAnalysis analyse(Key key, String... symbols) {
        return ProgressionAnalyzer.analyze(
                java.util.Arrays.stream(symbols).map(ChordAnalyzer::parse).toList(), key);
    }

    @Test
    @DisplayName("the Neapolitan is a flat-two major triad, and it is the same chord in either mode")
    void spellsTheNeapolitan() {
        assertEquals(List.of("Db", "F", "Ab"), chord("N6", Key.major("C")));
        assertEquals(List.of("Db", "F", "Ab"), chord("bII6", Key.minor("C")));
        assertEquals("F", RomanNumeralAnalyzer.realize("N6", Key.major("C")).bass().name());
    }

    @Test
    @DisplayName("a German sixth is spelled with an F sharp, which is what makes it not an A flat seventh")
    void spellsAugmentedSixths() {
        assertEquals(List.of("Ab", "C", "F#"), chord("It+6", Key.major("C")));
        assertEquals(List.of("Ab", "C", "D", "F#"), chord("Fr+6", Key.major("C")));
        assertEquals(List.of("Ab", "C", "Eb", "F#"), chord("Ger+6", Key.major("C")));
        assertNotEquals("Gb", chord("Ger+6", Key.major("C")).get(3));
        assertEquals(Interval.AUGMENTED_SIXTH,
                Interval.between(PitchClass.parse("Ab"), PitchClass.parse("F#")));
    }

    @Test
    void recognisesThemInAProgression() {
        assertEquals("I - bII6 - V - I", analyse(Key.major("C"), "C", "Db/F", "G", "C").romanNumeralLine());
        assertEquals("I - Ger+6 - V", analyse(Key.major("C"), "C", "Ab(Ger+6)", "G").romanNumeralLine());
        assertEquals("I - bVI - I", analyse(Key.major("C"), "C", "Ab", "C").romanNumeralLine());
    }

    @Test
    @DisplayName("a chord with no honest explanation is still marked, not guessed at")
    void doesNotOverreach() {
        // E flat MINOR on flat-three is not a borrowing anyone writes. The quality check in
        // chromaticChord is the only thing keeping this a question mark; do not loosen it.
        assertEquals("I - ?", analyse(Key.major("C"), "C", "Ebm").romanNumeralLine());
        assertEquals("I - ?", analyse(Key.major("C"), "C", "Ab7").romanNumeralLine(),
                "the enharmonic German sixth is left unclaimed rather than guessed");
    }

    @Test
    @DisplayName("a German sixth is not heard as a deceptive cadence just because it sits on the sixth degree")
    void doesNotMistakeChromaticChordsForCadences() {
        assertTrue(analyse(Key.major("C"), "G", "Ab(Ger+6)").cadences().isEmpty());
        assertTrue(analyse(Key.major("C"), "G", "Ab").cadences().isEmpty());
    }
}
