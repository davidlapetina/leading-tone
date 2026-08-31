package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
        assertTrue(abc.contains("^F"));
        assertTrue(abc.trim().endsWith("|]"));
    }

    @Test
    void rendersAProgression() {
        String abc = AbcNotation.progression(
                List.of(ChordAnalyzer.parse("C"), ChordAnalyzer.parse("G7")), Key.major("C"), 3);
        assertTrue(abc.contains("\"C\""));
        assertTrue(abc.contains("\"G7\""));
    }
}
