package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RomanNumeralTest {

    private static List<String> names(List<PitchClass> pitchClasses) {
        return pitchClasses.stream().map(PitchClass::name).toList();
    }

    private static RomanNumeralAnalysis analyse(String numeral, Key key) {
        return RomanNumeralAnalyzer.analyze(numeral, key);
    }

    @Test
    void parsesPlainNumerals() {
        assertEquals(1, RomanNumeral.parse("I").degree());
        assertEquals(ChordQuality.MAJOR, RomanNumeral.parse("IV").quality());
        assertEquals(ChordQuality.MINOR, RomanNumeral.parse("ii").quality());
        assertEquals(ChordQuality.DIMINISHED, RomanNumeral.parse("vii°").quality());
        assertEquals(ChordQuality.DIMINISHED, RomanNumeral.parse("viio").quality());
    }

    @Test
    @DisplayName("the figures say which inversion, and 7 means the seventh this key gives you")
    void parsesFiguredBass() {
        assertEquals(Inversion.FIRST, RomanNumeral.parse("V6").inversion());
        assertEquals(Inversion.SECOND, RomanNumeral.parse("I64").inversion());
        assertEquals(Inversion.FIRST, RomanNumeral.parse("V65").inversion());
        assertEquals(Inversion.SECOND, RomanNumeral.parse("V43").inversion());
        assertEquals(Inversion.THIRD, RomanNumeral.parse("V42").inversion());
        assertEquals(ChordQuality.DOMINANT_SEVENTH, RomanNumeral.parse("V7").quality());
        assertEquals(ChordQuality.MINOR_SEVENTH, RomanNumeral.parse("ii7").quality());
        assertEquals(ChordQuality.HALF_DIMINISHED_SEVENTH, RomanNumeral.parse("viiø7").quality());
        assertEquals(ChordQuality.DIMINISHED_SEVENTH, RomanNumeral.parse("vii°7").quality());
        assertEquals(ChordQuality.MAJOR_SEVENTH, RomanNumeral.parse("Imaj7").quality());
    }

    @Test
    void parsesChromaticRoots() {
        assertEquals(Accidental.FLAT, RomanNumeral.parse("bII").accidental());
        assertEquals(Accidental.FLAT, RomanNumeral.parse("bVI").accidental());
        assertEquals(Accidental.SHARP, RomanNumeral.parse("#iv°").accidental());
        assertEquals(4, RomanNumeral.parse("#iv°").degree());
    }

    @Test
    @DisplayName("a numeral survives being written out and read back")
    void roundTripsThroughItsSymbol() {
        for (String input : List.of("I", "ii", "vii°", "V7", "ii7", "viiø7", "V6", "I64",
                "V65", "V43", "V42", "V7/V", "V/ii", "vii°7/V", "bII", "bVI", "N6", "Ger+6")) {
            RomanNumeral once = RomanNumeral.parse(input);
            assertEquals(once, RomanNumeral.parse(once.symbol()), input + " -> " + once.symbol());
        }
    }

    @Test
    @DisplayName("V7/V in C major is D F sharp A C, and the third is F sharp, not G flat")
    void realisesAppliedDominants() {
        RomanNumeralAnalysis analysis = analyse("V7/V", Key.major("C"));

        assertEquals("D", analysis.root().name());
        assertEquals(List.of("D", "F#", "A", "C"), names(analysis.pitchClasses()));
        assertEquals(ChordQuality.DOMINANT_SEVENTH, analysis.quality());
        assertEquals(HarmonicFunction.APPLIED_DOMINANT, analysis.function());
        assertEquals(5, analysis.targetDegree());
        assertNotEquals("Gb", analysis.pitchClasses().get(1).name(), "spelling, not pitch class");
    }

    @Test
    @DisplayName("an applied chord borrows the key it points at, so V/ii has a C sharp")
    void realisesAppliedChordsInMinorTargets() {
        assertEquals(List.of("A", "C#", "E"), names(analyse("V/ii", Key.major("C")).pitchClasses()));
        assertEquals(List.of("F#", "A", "C", "Eb"), names(analyse("vii°7/V", Key.major("C")).pitchClasses()));
    }

    @Test
    void realisesTheDiatonicChordsTheSpecificationAsksFor() {
        Key c = Key.major("C");
        assertEquals(List.of("G", "B", "D"), names(analyse("V", c).pitchClasses()));
        assertEquals(List.of("G", "B", "D", "F"), names(analyse("V7", c).pitchClasses()));
        assertEquals(List.of("D", "F", "A"), names(analyse("ii", c).pitchClasses()));
        assertEquals(List.of("F", "Ab", "C"), names(analyse("iv", Key.minor("C")).pitchClasses()));
        assertEquals(List.of("E", "G#", "B", "D"), names(analyse("V7", Key.minor("A")).pitchClasses()));
    }

    @Test
    @DisplayName("a plain numeral takes the key's own degree: III in C minor is E flat")
    void keepsTheKeysOwnDegreeForPlainNumerals() {
        assertEquals(List.of("Eb", "G", "Bb"), names(analyse("III", Key.minor("C")).pitchClasses()));
    }

    @Test
    @DisplayName("an altered numeral is measured from the major scale, so flat-six is A flat in either mode")
    void measuresAccidentalsFromTheMajorScale() {
        assertEquals("Ab", analyse("bVI", Key.major("C")).root().name());
        assertEquals("Ab", analyse("bVI", Key.minor("C")).root().name(),
                "measuring from the key's own flattened sixth would give A double flat");
    }

    @Test
    @DisplayName("the specification's spelling cases, checked as spellings and not as pitch classes")
    void spellsTheStandardCases() {
        assertEquals(Interval.MAJOR_THIRD, Interval.between(PitchClass.parse("C"), PitchClass.parse("E")));
        assertEquals(Interval.MINOR_THIRD, Interval.between(PitchClass.parse("C"), PitchClass.parse("Eb")));
        assertEquals(Interval.AUGMENTED_FOURTH, Interval.between(PitchClass.parse("C"), PitchClass.parse("F#")));
        assertEquals(Interval.DIMINISHED_FIFTH, Interval.between(PitchClass.parse("C"), PitchClass.parse("Gb")));
        assertNotEquals(Interval.between(PitchClass.parse("C"), PitchClass.parse("F#")),
                Interval.between(PitchClass.parse("C"), PitchClass.parse("Gb")),
                "an augmented fourth and a diminished fifth sound alike and are not the same interval");

        assertEquals(List.of("C", "D", "E", "F", "G", "A", "B"),
                names(Scale.of("C", ScaleType.MAJOR).pitchClasses()));
        assertEquals(List.of("F#", "G#", "A#", "B", "C#", "D#", "E#"),
                names(Scale.of("F#", ScaleType.MAJOR).pitchClasses()));
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G#"),
                names(Scale.of("A", ScaleType.HARMONIC_MINOR).pitchClasses()));

        assertEquals(List.of("D", "F#", "A"), names(analyse("V/V", Key.major("C")).pitchClasses()));
        assertEquals(List.of("E", "G#", "B"), names(analyse("V", Key.minor("A")).pitchClasses()));
    }

    @Test
    void readsTheSpellingsCorporaActuallyUse() {
        assertTrue(RomanNumeral.tryParse("viio7").isPresent());
        assertTrue(RomanNumeral.tryParse("Ger6").isPresent());
        assertTrue(RomanNumeral.tryParse("V(64)").isPresent());
        assertTrue(RomanNumeral.tryParse("@none").isEmpty());
        assertTrue(RomanNumeral.tryParse("").isEmpty());
        assertTrue(RomanNumeral.tryParse("nonsense").isEmpty());
    }
}
