package fr.lapetina.music.theory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record Chord(PitchClass root, ChordQuality quality, Inversion inversion) {

    public Chord {
        if (inversion.index() >= quality.size()) {
            throw new IllegalArgumentException(
                    quality.displayName() + " has no " + inversion.displayName());
        }
    }

    public static Chord of(PitchClass root, ChordQuality quality) {
        return new Chord(root, quality, Inversion.ROOT_POSITION);
    }

    public static Chord of(String root, ChordQuality quality) {
        return of(PitchClass.parse(root), quality);
    }

    public static Chord of(String root, ChordQuality quality, Inversion inversion) {
        return new Chord(PitchClass.parse(root), quality, inversion);
    }

    /** Chord tones stacked from the root upwards, regardless of inversion. */
    public List<PitchClass> pitchClasses() {
        List<PitchClass> result = new ArrayList<>(quality.size());
        for (Interval interval : quality.intervals()) {
            result.add(root.transpose(interval));
        }
        return List.copyOf(result);
    }

    public Set<PitchClass> pitchClassSet() {
        return new LinkedHashSet<>(pitchClasses());
    }

    public Set<Integer> semitoneSet() {
        Set<Integer> result = new LinkedHashSet<>();
        for (PitchClass pitchClass : pitchClasses()) {
            result.add(pitchClass.semitone());
        }
        return result;
    }

    public PitchClass bass() {
        return pitchClasses().get(inversion.index());
    }

    public PitchClass third() {
        return pitchClasses().get(1);
    }

    public PitchClass fifth() {
        return pitchClasses().get(2);
    }

    public Chord inverted(Inversion target) {
        return new Chord(root, quality, target);
    }

    /** Voiced upwards from the bass note, which is what a keyboard exercise expects. */
    public List<Note> notes(int bassOctave) {
        List<PitchClass> ordered = pitchClasses();
        List<Note> result = new ArrayList<>(ordered.size());
        int octave = bassOctave;
        int previousMidi = Integer.MIN_VALUE;
        for (int i = 0; i < ordered.size(); i++) {
            PitchClass pitchClass = ordered.get((inversion.index() + i) % ordered.size());
            Note note = new Note(pitchClass, octave);
            while (note.midi() <= previousMidi) {
                octave++;
                note = new Note(pitchClass, octave);
            }
            result.add(note);
            previousMidi = note.midi();
        }
        return List.copyOf(result);
    }

    /** Lead-sheet symbol, with a slash bass when inverted: {@code G/B}. */
    /** The same chord, moved. Spelling follows the letters, so Dm7 up a fourth is Gm7. */
    public Chord transpose(Interval interval) {
        return new Chord(root.transpose(interval), quality, inversion);
    }

    public String symbol() {
        if (quality == ChordQuality.ITALIAN_SIXTH || quality == ChordQuality.FRENCH_SIXTH
                || quality == ChordQuality.GERMAN_SIXTH) {
            // Written the way a textbook writes it: Ab(Ger+6), not AbGer+6.
            return root.name() + "(" + quality.symbol() + ")";
        }
        String base = root.name() + quality.symbol();
        return inversion == Inversion.ROOT_POSITION ? base : base + "/" + bass().name();
    }

    public String describe() {
        String base = root.name() + " " + quality.displayName();
        return inversion == Inversion.ROOT_POSITION ? base : base + " in " + inversion.displayName();
    }

    @Override
    public String toString() {
        return symbol();
    }
}
