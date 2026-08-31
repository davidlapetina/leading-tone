package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PitchAndIntervalTest {

    @Test
    void parsesAndSpellsPitchClasses() {
        assertEquals(6, PitchClass.parse("F#").semitone());
        assertEquals(6, PitchClass.parse("Gb").semitone());
        assertEquals("F#", PitchClass.parse("F#").name());
        assertEquals("Gb", PitchClass.parse("Gb").name());
        assertEquals(0, PitchClass.parse("B#").semitone());
        assertEquals(11, PitchClass.parse("Cb").semitone());
    }

    @Test
    @DisplayName("enharmonic pitches share a semitone but stay different objects")
    void enharmonicsAreNotEqual() {
        PitchClass fSharp = PitchClass.parse("F#");
        PitchClass gFlat = PitchClass.parse("Gb");
        assertTrue(fSharp.isEnharmonicWith(gFlat));
        assertFalse(fSharp.equals(gFlat));
    }

    @Test
    void transposesBySpelledInterval() {
        PitchClass c = PitchClass.parse("C");
        assertEquals("F#", c.transpose(Interval.AUGMENTED_FOURTH).name());
        assertEquals("Gb", c.transpose(Interval.DIMINISHED_FIFTH).name());
        assertEquals("E", c.transpose(Interval.MAJOR_THIRD).name());
        assertEquals("Eb", c.transpose(Interval.MINOR_THIRD).name());
        assertEquals("A#", PitchClass.parse("F#").transpose(Interval.MAJOR_THIRD).name());
        assertEquals("Bb", PitchClass.parse("Gb").transpose(Interval.MAJOR_THIRD).name());
    }

    @Test
    void measuresIntervalsFromSpelling() {
        assertEquals(Interval.MAJOR_THIRD, Interval.between(PitchClass.parse("C"), PitchClass.parse("E")));
        assertEquals(Interval.AUGMENTED_FOURTH, Interval.between(PitchClass.parse("C"), PitchClass.parse("F#")));
        assertEquals(Interval.DIMINISHED_FIFTH, Interval.between(PitchClass.parse("C"), PitchClass.parse("Gb")));
        assertEquals(Interval.MINOR_SEVENTH, Interval.between(PitchClass.parse("G"), PitchClass.parse("F")));
        assertEquals(Interval.MAJOR_SEVENTH, Interval.between(PitchClass.parse("C"), PitchClass.parse("B")));
        assertEquals(Interval.MINOR_SECOND, Interval.between(PitchClass.parse("B"), PitchClass.parse("C")));
    }

    @Test
    void intervalSemitonesFollowQuality() {
        assertEquals(4, Interval.MAJOR_THIRD.semitones());
        assertEquals(3, Interval.MINOR_THIRD.semitones());
        assertEquals(6, Interval.AUGMENTED_FOURTH.semitones());
        assertEquals(6, Interval.DIMINISHED_FIFTH.semitones());
        assertEquals(10, Interval.MINOR_SEVENTH.semitones());
        assertEquals(9, Interval.DIMINISHED_SEVENTH.semitones());
        assertEquals(12, Interval.PERFECT_OCTAVE.semitones());
    }

    @Test
    void rejectsImpossibleQualities() {
        assertThrows(IllegalArgumentException.class, () -> new Interval(5, IntervalQuality.MAJOR));
        assertThrows(IllegalArgumentException.class, () -> new Interval(3, IntervalQuality.PERFECT));
    }

    @Test
    void convertsBetweenNotesAndMidi() {
        assertEquals(60, Note.parse("C4").midi());
        assertEquals(69, Note.parse("A4").midi());
        assertEquals(52, Note.parse("E3").midi());
        assertEquals(56, Note.parse("G#3").midi());
        assertEquals("C4", Note.fromMidi(60).name());
        assertEquals("Eb4", Note.fromMidi(63).name());
    }

    @Test
    @DisplayName("a key context spells MIDI notes the way the key does")
    void spellsMidiInsideAKey() {
        assertEquals("D#4", Note.fromMidi(63, Key.minor("E")).name());
        assertEquals("Eb4", Note.fromMidi(63, Key.major("Bb")).name());
    }
}
