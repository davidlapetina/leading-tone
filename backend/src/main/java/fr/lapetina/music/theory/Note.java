package fr.lapetina.music.theory;

import java.util.List;
import java.util.Objects;

/**
 * A pitch class placed in an octave, in scientific pitch notation where middle C is C4
 * and MIDI note 60.
 */
public record Note(PitchClass pitchClass, int octave) implements Comparable<Note> {

    /** Default spelling used when a MIDI number arrives with no key context. */
    private static final String[] DEFAULT_SPELLING = {
            "C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
    };

    public Note {
        Objects.requireNonNull(pitchClass, "pitchClass");
    }

    public static Note of(String pitchClass, int octave) {
        return new Note(PitchClass.parse(pitchClass), octave);
    }

    public int midi() {
        return (octave + 1) * 12 + pitchClass.semitone();
    }

    public static Note fromMidi(int midi) {
        PitchClass spelled = PitchClass.parse(DEFAULT_SPELLING[Math.floorMod(midi, 12)]);
        return new Note(spelled, midi / 12 - 1);
    }

    /**
     * Spells a MIDI number inside a key. The natural scale is tried first, then the
     * raised-seventh form, so a D#4 in E minor is not flattened into an Eb4.
     */
    public static Note fromMidi(int midi, Key key) {
        int semitone = Math.floorMod(midi, 12);
        for (Scale candidateScale : List.of(key.scale(), key.scale(true))) {
            for (PitchClass candidate : candidateScale.pitchClasses()) {
                if (candidate.semitone() == semitone) {
                    return new Note(candidate, midi / 12 - 1);
                }
            }
        }
        return fromMidi(midi);
    }

    /** Keeps the octave right across the boundary: B4 up a minor second is C5, not C4. */
    public Note transpose(Interval interval) {
        PitchClass moved = pitchClass.transpose(interval);
        int semitones = midi() + interval.semitones();
        int octave = Math.floorDiv(semitones - moved.semitone(), 12) - 1;
        return new Note(moved, octave);
    }

    public String name() {
        return pitchClass.name() + octave;
    }

    /** Parses names such as {@code C4}, {@code F#3} or {@code Bb-1}. */
    public static Note parse(String text) {
        String trimmed = text.trim();
        int index = 1;
        while (index < trimmed.length() && !Character.isDigit(trimmed.charAt(index)) && trimmed.charAt(index) != '-') {
            index++;
        }
        return new Note(PitchClass.parse(trimmed.substring(0, index)), Integer.parseInt(trimmed.substring(index)));
    }

    @Override
    public int compareTo(Note other) {
        return Integer.compare(midi(), other.midi());
    }

    @Override
    public String toString() {
        return name();
    }
}
