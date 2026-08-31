package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScaleAndKeyTest {

    private static List<String> names(List<PitchClass> pitchClasses) {
        return pitchClasses.stream().map(PitchClass::name).toList();
    }

    @Test
    void buildsMajorScales() {
        assertEquals(List.of("C", "D", "E", "F", "G", "A", "B"), names(Scale.of("C", ScaleType.MAJOR).pitchClasses()));
        assertEquals(List.of("D", "E", "F#", "G", "A", "B", "C#"), names(Scale.of("D", ScaleType.MAJOR).pitchClasses()));
        assertEquals(List.of("Bb", "C", "D", "Eb", "F", "G", "A"), names(Scale.of("Bb", ScaleType.MAJOR).pitchClasses()));
    }

    @Test
    @DisplayName("F# major keeps its E#, it does not collapse to F")
    void keepsAwkwardSpellings() {
        assertEquals(List.of("F#", "G#", "A#", "B", "C#", "D#", "E#"),
                names(Scale.of("F#", ScaleType.MAJOR).pitchClasses()));
        assertEquals(List.of("Cb", "Db", "Eb", "Fb", "Gb", "Ab", "Bb"),
                names(Scale.of("Cb", ScaleType.MAJOR).pitchClasses()));
    }

    @Test
    void buildsMinorScaleVariants() {
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G"),
                names(Scale.of("A", ScaleType.NATURAL_MINOR).pitchClasses()));
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G#"),
                names(Scale.of("A", ScaleType.HARMONIC_MINOR).pitchClasses()));
        assertEquals(List.of("A", "B", "C", "D", "E", "F#", "G#"),
                names(Scale.of("A", ScaleType.MELODIC_MINOR).pitchClasses()));
    }

    @Test
    void buildsModes() {
        assertEquals(List.of("D", "E", "F", "G", "A", "B", "C"), names(Scale.of("D", ScaleType.DORIAN).pitchClasses()));
        assertEquals(List.of("F", "G", "A", "B", "C", "D", "E"), names(Scale.of("F", ScaleType.LYDIAN).pitchClasses()));
        assertEquals(List.of("G", "A", "B", "C", "D", "E", "F"),
                names(Scale.of("G", ScaleType.MIXOLYDIAN).pitchClasses()));
    }

    @Test
    void spellsAscendingNotesAcrossTheOctaveBreak() {
        assertEquals(List.of("B4", "C#5", "D#5", "E5", "F#5", "G#5", "A#5", "B5"),
                Scale.of("B", ScaleType.MAJOR).notes(4).stream().map(Note::name).toList());
    }

    @Test
    void derivesKeySignatures() {
        assertEquals(0, Key.major("C").keySignature());
        assertEquals(2, Key.major("D").keySignature());
        assertEquals(5, Key.major("B").keySignature());
        assertEquals(-1, Key.major("F").keySignature());
        assertEquals(-5, Key.major("Db").keySignature());
        assertEquals(0, Key.minor("A").keySignature());
        assertEquals(-1, Key.minor("D").keySignature());
    }

    @Test
    void buildsDiatonicTriadsInMajor() {
        assertEquals(List.of("C", "Dm", "Em", "F", "G", "Am", "Bdim"),
                Key.major("C").diatonicTriads().stream().map(Chord::symbol).toList());
    }

    @Test
    @DisplayName("minor keys raise the seventh for V and vii, but nowhere else")
    void buildsDiatonicTriadsInMinor() {
        assertEquals(List.of("Am", "Bdim", "C", "Dm", "E", "F", "G#dim"),
                Key.minor("A").diatonicTriads().stream().map(Chord::symbol).toList());
        assertEquals(List.of("Am", "Bdim", "C", "Dm", "Em", "F", "G"),
                Key.minor("A").diatonicTriads(false).stream().map(Chord::symbol).toList());
    }

    @Test
    void buildsDiatonicSevenths() {
        assertEquals(List.of("Cmaj7", "Dm7", "Em7", "Fmaj7", "G7", "Am7", "Bm7b5"),
                Key.major("C").diatonicSevenths().stream().map(Chord::symbol).toList());
    }

    @Test
    void knowsRelativeAndParallelKeys() {
        assertEquals("A minor", Key.major("C").relative().name());
        assertEquals("Eb major", Key.minor("C").relative().name());
        assertEquals("C minor", Key.major("C").parallel().name());
        assertTrue(Key.major("D").contains(PitchClass.parse("C#")));
    }

    @Test
    void namesTheDominantOfAMinorKey() {
        assertEquals("E", Key.minor("A").dominantTriad().symbol());
        assertEquals("E7", Key.minor("A").dominantSeventh().symbol());
        assertEquals("G#", Key.minor("A").leadingTone().name());
    }
}
