package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JazzScaleTest {

    private static List<String> names(String tonic, ScaleType type) {
        return Scale.of(tonic, type).pitchClasses().stream().map(PitchClass::name).toList();
    }

    @Test
    void spellsPentatonicsAndBlues() {
        assertEquals(List.of("C", "D", "E", "G", "A"), names("C", ScaleType.MAJOR_PENTATONIC));
        assertEquals(List.of("C", "Eb", "F", "G", "Bb"), names("C", ScaleType.MINOR_PENTATONIC));
        assertEquals(List.of("C", "Eb", "F", "Gb", "G", "Bb"), names("C", ScaleType.BLUES));
    }

    @Test
    @DisplayName("the whole tone scale ends on A sharp: it is an augmented sixth above C, not a seventh")
    void spellsWholeTone() {
        assertEquals(List.of("C", "D", "E", "F#", "G#", "A#"), names("C", ScaleType.WHOLE_TONE));
        assertNotEquals("Bb", names("C", ScaleType.WHOLE_TONE).get(5));
        assertEquals(List.of("Gb", "Ab", "Bb", "C", "D", "E"), names("Gb", ScaleType.WHOLE_TONE));
    }

    @Test
    @DisplayName("the altered scale has a diminished fourth, so G altered contains C flat, not B")
    void spellsAltered() {
        assertEquals(List.of("G", "Ab", "Bb", "Cb", "Db", "Eb", "F"), names("G", ScaleType.ALTERED));
        assertEquals(List.of("C", "Db", "Eb", "Fb", "Gb", "Ab", "Bb"), names("C", ScaleType.ALTERED));
    }

    @Test
    void spellsBothOctatonics() {
        assertEquals(List.of("C", "Db", "Eb", "E", "F#", "G", "A", "Bb"),
                names("C", ScaleType.OCTATONIC_HALF_WHOLE));
        assertEquals(List.of("C", "D", "Eb", "F", "Gb", "Ab", "A", "B"),
                names("C", ScaleType.OCTATONIC_WHOLE_HALF));
    }

    @Test
    @DisplayName("a scale that is not seven notes long says so, because key signatures assume seven")
    void knowsWhichScalesAreHeptatonic() {
        assertTrue(ScaleType.MAJOR.isHeptatonic());
        assertTrue(ScaleType.ALTERED.isHeptatonic());
        assertFalse(ScaleType.BLUES.isHeptatonic());
        assertFalse(ScaleType.WHOLE_TONE.isHeptatonic());
        assertFalse(ScaleType.OCTATONIC_HALF_WHOLE.isHeptatonic());
    }

    @Test
    @DisplayName("a whole tone scale is not written under a minor key signature")
    void writesNonHeptatonicScalesWithoutAKeySignature() {
        String abc = AbcNotation.scale(Scale.of("C", ScaleType.WHOLE_TONE), 4);

        assertTrue(abc.contains("K:C"), abc);
        assertFalse(abc.contains("K:Cm"), abc);
        assertEquals(7, Scale.of("C", ScaleType.WHOLE_TONE).notes(4).size(), "six notes plus the octave");
    }

    @Test
    void stillWritesOrdinaryScalesTheSameWay() {
        assertTrue(AbcNotation.scale(Scale.of("D", ScaleType.MAJOR), 4).contains("K:D"));
    }
}
