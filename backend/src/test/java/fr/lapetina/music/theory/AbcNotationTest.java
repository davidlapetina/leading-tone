package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AbcNotationTest {

    @Test
    void writesPitchesInAbcOctaveNotation() {
        assertEquals("C", AbcNotation.pitch(Note.parse("C4")));
        assertEquals("c", AbcNotation.pitch(Note.parse("C5")));
        assertEquals("c'", AbcNotation.pitch(Note.parse("C6")));
        assertEquals("C,", AbcNotation.pitch(Note.parse("C3")));
        assertEquals("^F", AbcNotation.pitch(Note.parse("F#4")));
        assertEquals("_B,", AbcNotation.pitch(Note.parse("Bb3")));
    }

    @Test
    void writesKeyFields() {
        assertEquals("C", AbcNotation.keyField(Key.major("C")));
        assertEquals("F#m", AbcNotation.keyField(Key.minor("F#")));
    }

    @Test
    void rendersAChordWithItsSymbol() {
        String abc = AbcNotation.chord(Chord.of("G", ChordQuality.MAJOR, Inversion.FIRST), 3, Key.major("C"));
        assertTrue(abc.contains("K:C"));
        assertTrue(abc.contains("\"G/B\""));
        assertTrue(abc.contains("[B,DG]"));
    }

    @Test
    void rendersAScale() {
        String abc = AbcNotation.scale(Scale.of("D", ScaleType.MAJOR), 4);
        assertTrue(abc.contains("K:D"));
        // F sharp comes from the key signature; writing it again on every F is not engraving.
        assertFalse(abc.contains("^F"), abc);
        assertTrue(abc.trim().endsWith("|]"));
    }

    @Test
    @DisplayName("an accidental the key signature does not supply is written")
    void writesWhatTheSignatureDoesNot() {
        // A harmonic minor has no sharps in its signature, so its raised seventh is written.
        String harmonic = AbcNotation.scale(Scale.of("A", ScaleType.HARMONIC_MINOR), 4);
        assertTrue(harmonic.contains("^g"), "the raised seventh sits in the octave above: " + harmonic);

        // G Dorian was drawn as G Aeolian: under a two-flat signature the E natural was
        // written with no sign at all, and read as E flat -- a different mode.
        String dorian = AbcNotation.scale(Scale.of("G", ScaleType.DORIAN), 4);
        assertTrue(dorian.contains("=e"), "the natural sixth is what makes it Dorian: " + dorian);
        assertFalse(dorian.contains("_B"), "the flat second comes from the signature: " + dorian);
    }

    @Test
    void rendersAProgression() {
        String abc = AbcNotation.progression(
                List.of(ChordAnalyzer.parse("C"), ChordAnalyzer.parse("G7")), Key.major("C"), 3);
        assertTrue(abc.contains("\"C\""));
        assertTrue(abc.contains("\"G7\""));
    }
}
