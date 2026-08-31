package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChordAnalyzerTest {

    @Test
    @DisplayName("the lowest played note decides the inversion")
    void readsInversionFromTheBass() {
        Chord firstInversion = ChordAnalyzer.fromMidi(List.of(59, 62, 67)).orElseThrow();
        assertEquals("G", firstInversion.root().name());
        assertEquals(ChordQuality.MAJOR, firstInversion.quality());
        assertEquals(Inversion.FIRST, firstInversion.inversion());

        Chord rootPosition = ChordAnalyzer.fromMidi(List.of(55, 59, 62)).orElseThrow();
        assertEquals(Inversion.ROOT_POSITION, rootPosition.inversion());
    }

    @Test
    @DisplayName("spread voicings and octave doubling do not change the analysis")
    void ignoresSpacingAndDoubling() {
        Chord spread = ChordAnalyzer.fromMidi(List.of(59, 67, 74)).orElseThrow();
        assertEquals("G/B", spread.symbol());
        Chord doubled = ChordAnalyzer.fromMidi(List.of(59, 62, 67, 71, 79)).orElseThrow();
        assertEquals("G/B", doubled.symbol());
    }

    @Test
    void identifiesSeventhsAndAlteredTriads() {
        assertEquals("G7", ChordAnalyzer.fromMidi(List.of(55, 59, 62, 65)).orElseThrow().symbol());
        assertEquals("Caug", ChordAnalyzer.fromMidi(List.of(60, 64, 68)).orElseThrow().symbol());
        assertEquals("Bdim7", ChordAnalyzer.fromMidi(List.of(59, 62, 65, 68)).orElseThrow().symbol());
        assertEquals("Csus4", ChordAnalyzer.fromMidi(List.of(60, 65, 67)).orElseThrow().symbol());
        assertEquals("Bm7b5", ChordAnalyzer.fromMidi(List.of(59, 62, 65, 69)).orElseThrow().symbol());
    }

    @Test
    @DisplayName("a key context picks the spelling the key would use")
    void spellsInsideAKey() {
        Chord inSharpKey = ChordAnalyzer.fromMidi(List.of(56, 59, 62), Key.minor("A")).orElseThrow();
        assertEquals("G#dim", inSharpKey.symbol());
        Chord withoutKey = ChordAnalyzer.fromMidi(List.of(63, 67, 70)).orElseThrow();
        assertEquals("Eb", withoutKey.symbol());
    }

    @Test
    void returnsEmptyForNonChords() {
        assertEquals(Optional.empty(), ChordAnalyzer.fromMidi(List.of(60, 61, 62)));
        assertEquals(Optional.empty(), ChordAnalyzer.fromMidi(List.of()));
    }

    @Test
    void analysesSpelledNotes() {
        Chord chord = ChordAnalyzer.fromNotes(List.of(Note.parse("E3"), Note.parse("G#3"), Note.parse("B3")))
                .orElseThrow();
        assertEquals("E", chord.symbol());
        assertTrue(chord.pitchClasses().contains(PitchClass.parse("G#")));
    }

    @Test
    void parsesLeadSheetSymbols() {
        assertEquals(ChordQuality.MAJOR, ChordAnalyzer.parse("C").quality());
        assertEquals(ChordQuality.MINOR, ChordAnalyzer.parse("F#m").quality());
        assertEquals(ChordQuality.DOMINANT_SEVENTH, ChordAnalyzer.parse("Bb7").quality());
        assertEquals(ChordQuality.MAJOR_SEVENTH, ChordAnalyzer.parse("Cmaj7").quality());
        assertEquals(ChordQuality.HALF_DIMINISHED_SEVENTH, ChordAnalyzer.parse("Bm7b5").quality());
        assertEquals(Inversion.FIRST, ChordAnalyzer.parse("G/B").inversion());
    }
}
