package fr.lapetina.music.theory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scale(PitchClass tonic, ScaleType type) {

    public static Scale of(String tonic, ScaleType type) {
        return new Scale(PitchClass.parse(tonic), type);
    }

    public List<PitchClass> pitchClasses() {
        List<PitchClass> result = new ArrayList<>(type.degreeCount());
        for (Interval interval : type.pattern()) {
            result.add(tonic.transpose(interval));
        }
        return List.copyOf(result);
    }

    /** Degrees are 1-based: {@code degree(5)} of C major is G. */
    public PitchClass degree(int degree) {
        List<PitchClass> classes = pitchClasses();
        return classes.get(Math.floorMod(degree - 1, classes.size()));
    }

    public Optional<Integer> degreeOf(PitchClass pitchClass) {
        List<PitchClass> classes = pitchClasses();
        for (int i = 0; i < classes.size(); i++) {
            if (classes.get(i).equals(pitchClass)) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }


    public boolean contains(PitchClass pitchClass) {
        return degreeOf(pitchClass).isPresent();
    }

    /** Ascending notes starting in {@code octave}, with the tonic repeated on top. */
    public List<Note> notes(int octave) {
        List<Note> result = new ArrayList<>();
        int previousMidi = Integer.MIN_VALUE;
        int currentOctave = octave;
        for (PitchClass pitchClass : pitchClasses()) {
            Note note = new Note(pitchClass, currentOctave);
            if (previousMidi != Integer.MIN_VALUE && note.midi() <= previousMidi) {
                currentOctave++;
                note = new Note(pitchClass, currentOctave);
            }
            result.add(note);
            previousMidi = note.midi();
        }
        Note top = new Note(tonic, currentOctave);
        while (top.midi() <= previousMidi) {
            currentOctave++;
            top = new Note(tonic, currentOctave);
        }
        result.add(top);
        return List.copyOf(result);
    }

    public Scale transpose(Interval interval) {
        return new Scale(tonic.transpose(interval), type);
    }

    public String name() {
        return tonic.name() + " " + type.displayName().toLowerCase();
    }

    @Override
    public String toString() {
        return name();
    }
}
