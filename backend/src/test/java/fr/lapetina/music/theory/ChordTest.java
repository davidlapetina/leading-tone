package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChordTest {

    private static List<String> names(List<PitchClass> pitchClasses) {
        return pitchClasses.stream().map(PitchClass::name).toList();
    }

    @Test
    void buildsChordsFromRootAndQuality() {
        assertEquals(List.of("C", "E", "G"), names(Chord.of("C", ChordQuality.MAJOR).pitchClasses()));
        assertEquals(List.of("C", "Eb", "G"), names(Chord.of("C", ChordQuality.MINOR).pitchClasses()));
        assertEquals(List.of("B", "D", "F"), names(Chord.of("B", ChordQuality.DIMINISHED).pitchClasses()));
        assertEquals(List.of("C", "E", "G#"), names(Chord.of("C", ChordQuality.AUGMENTED).pitchClasses()));
        assertEquals(List.of("G", "B", "D", "F"), names(Chord.of("G", ChordQuality.DOMINANT_SEVENTH).pitchClasses()));
        assertEquals(List.of("B", "D", "F", "Ab"), names(Chord.of("B", ChordQuality.DIMINISHED_SEVENTH).pitchClasses()));
    }

    @Test
    void namesInversionsWithASlashBass() {
        Chord firstInversion = Chord.of("G", ChordQuality.MAJOR, Inversion.FIRST);
        assertEquals("G/B", firstInversion.symbol());
        assertEquals("B", firstInversion.bass().name());
        assertEquals("G major triad in first inversion", firstInversion.describe());
    }

    @Test
    @DisplayName("a first-inversion triad voices from the third upwards")
    void voicesInversionsFromTheBass() {
        assertEquals(List.of("B3", "D4", "G4"),
                Chord.of("G", ChordQuality.MAJOR, Inversion.FIRST).notes(3).stream().map(Note::name).toList());
        assertEquals(List.of("G3", "B3", "D4"),
                Chord.of("G", ChordQuality.MAJOR).notes(3).stream().map(Note::name).toList());
        assertEquals(List.of("D4", "G4", "B4"),
                Chord.of("G", ChordQuality.MAJOR, Inversion.SECOND).notes(4).stream().map(Note::name).toList());
    }

    @Test
    void figuresDependOnChordSize() {
        assertEquals("", Inversion.ROOT_POSITION.figuredBass(3));
        assertEquals("6", Inversion.FIRST.figuredBass(3));
        assertEquals("64", Inversion.SECOND.figuredBass(3));
        assertEquals("7", Inversion.ROOT_POSITION.figuredBass(4));
        assertEquals("65", Inversion.FIRST.figuredBass(4));
        assertEquals("43", Inversion.SECOND.figuredBass(4));
        assertEquals("42", Inversion.THIRD.figuredBass(4));
    }

    @Test
    void rejectsAThirdInversionOfATriad() {
        assertThrows(IllegalArgumentException.class,
                () -> new Chord(PitchClass.parse("C"), ChordQuality.MAJOR, Inversion.THIRD));
    }

    @Test
    void identifiesQualityFromMembers() {
        Optional<ChordQuality> quality = ChordQuality.identify(PitchClass.parse("C"),
                Set.of(PitchClass.parse("C"), PitchClass.parse("E"), PitchClass.parse("G")));
        assertEquals(Optional.of(ChordQuality.MAJOR), quality);
        assertTrue(ChordQuality.DOMINANT_SEVENTH.isSeventh());
        assertTrue(ChordQuality.DOMINANT_SEVENTH.isDominantFunctioning());
    }
}
