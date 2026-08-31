package fr.lapetina.music.midi;

import fr.lapetina.music.theory.Note;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What the learner actually played: the sounding MIDI numbers, in the order they were
 * struck.
 *
 * <p>Order is kept because the lowest note decides the inversion and the sequence decides
 * a scale. Velocity and timing are not carried in V1, because nothing evaluates them yet.
 */
public record MidiPerformance(List<Integer> notes) {

    public static MidiPerformance of(List<Integer> notes) {
        return new MidiPerformance(List.copyOf(notes));
    }

    public boolean isEmpty() {
        return notes.isEmpty();
    }

    public int lowestNote() {
        return notes.stream().min(Integer::compareTo).orElseThrow();
    }

    public int bassPitchClass() {
        return Math.floorMod(lowestNote(), 12);
    }

    /** Pitch classes actually sounding, with octave doubling collapsed. */
    public Set<Integer> pitchClasses() {
        Set<Integer> result = new LinkedHashSet<>();
        for (int note : notes) {
            result.add(Math.floorMod(note, 12));
        }
        return result;
    }

    /** Pitch classes in the order first played, which is what a scale answer needs. */
    public List<Integer> pitchClassSequence() {
        return notes.stream().map(note -> Math.floorMod(note, 12)).toList();
    }

    public List<Note> spelled() {
        return notes.stream().map(Note::fromMidi).toList();
    }
}
