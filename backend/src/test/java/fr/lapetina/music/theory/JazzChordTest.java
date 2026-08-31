package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JazzChordTest {

    private static List<String> names(Chord chord) {
        return chord.pitchClasses().stream().map(PitchClass::name).toList();
    }

    @Test
    void spellsSixthsAndExtensions() {
        assertEquals(List.of("C", "E", "G", "A"), names(Chord.of("C", ChordQuality.MAJOR_SIXTH)));
        assertEquals(List.of("C", "Eb", "G", "A"), names(Chord.of("C", ChordQuality.MINOR_SIXTH)));
        assertEquals(List.of("C", "E", "G", "Bb", "D"), names(Chord.of("C", ChordQuality.DOMINANT_NINTH)));
        assertEquals(List.of("C", "E", "G", "B", "D"), names(Chord.of("C", ChordQuality.MAJOR_NINTH)));
        assertEquals(List.of("C", "Eb", "G", "Bb", "D"), names(Chord.of("C", ChordQuality.MINOR_NINTH)));
        assertEquals(List.of("C", "E", "Bb", "D", "A"), names(Chord.of("C", ChordQuality.DOMINANT_THIRTEENTH)));
    }

    @Test
    @DisplayName("altered tensions keep their spelling: a #9 is D#, not Eb")
    void spellsAlteredDominants() {
        assertEquals(List.of("C", "E", "G", "Bb", "Db"), names(Chord.of("C", ChordQuality.DOMINANT_FLAT_NINTH)));
        assertEquals(List.of("C", "E", "G", "Bb", "D#"), names(Chord.of("C", ChordQuality.DOMINANT_SHARP_NINTH)));
        assertEquals(List.of("C", "E", "G", "Bb", "F#"),
                names(Chord.of("C", ChordQuality.DOMINANT_SHARP_ELEVENTH)));
        assertEquals(List.of("C", "E", "G", "Bb", "Ab"),
                names(Chord.of("C", ChordQuality.DOMINANT_FLAT_THIRTEENTH)));
    }

    @Test
    void knowsWhichExtensionsLeanTowardsResolution() {
        assertTrue(ChordQuality.DOMINANT_SHARP_NINTH.isAltered());
        assertTrue(ChordQuality.DOMINANT_SHARP_NINTH.isDominantFunctioning());
        assertTrue(ChordQuality.DOMINANT_THIRTEENTH.isExtended());
        assertTrue(ChordQuality.DOMINANT_THIRTEENTH.isDominantFunctioning());
        org.junit.jupiter.api.Assertions.assertFalse(ChordQuality.MAJOR_NINTH.isAltered());
        org.junit.jupiter.api.Assertions.assertFalse(ChordQuality.MAJOR_SIXTH.isExtended());
    }

    @Test
    @DisplayName("an extended chord is still recognised from what is actually played")
    void identifiesExtensionsFromMidi() {
        // C E G Bb D
        assertEquals("C9", ChordAnalyzer.fromMidi(List.of(48, 52, 55, 58, 62)).orElseThrow().symbol());
        // C E G A
        assertEquals("C6", ChordAnalyzer.fromMidi(List.of(48, 52, 55, 57)).orElseThrow().symbol());
    }

    @Test
    void parsesJazzChordSymbols() {
        assertEquals(ChordQuality.DOMINANT_NINTH, ChordAnalyzer.parse("C9").quality());
        assertEquals(ChordQuality.MAJOR_NINTH, ChordAnalyzer.parse("Ebmaj9").quality());
        assertEquals(ChordQuality.MINOR_NINTH, ChordAnalyzer.parse("F#m9").quality());
        assertEquals(ChordQuality.DOMINANT_SHARP_NINTH, ChordAnalyzer.parse("Bb7#9").quality());
        assertEquals(ChordQuality.MAJOR_SIXTH, ChordAnalyzer.parse("G6").quality());
    }
}
