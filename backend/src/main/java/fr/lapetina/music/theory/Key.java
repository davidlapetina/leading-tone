package fr.lapetina.music.theory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record Key(PitchClass tonic, Mode mode) {

    public static Key of(String tonic, Mode mode) {
        return new Key(PitchClass.parse(tonic), mode);
    }

    public static Key major(String tonic) {
        return of(tonic, Mode.MAJOR);
    }

    public static Key minor(String tonic) {
        return of(tonic, Mode.MINOR);
    }

    /**
     * Reads a key the way people write one: {@code C}, {@code C major}, {@code f# minor},
     * {@code Bbm}, {@code Eb_major}.
     *
     * <p>Deliberately lenient about the second word. Anything that is not recognisably
     * minor is read as major, because this is fed by a language model and by URL segments,
     * and answering "C ionian" with C major is more useful than answering with an error.
     * The tonic is not lenient: an unreadable tonic throws.
     */
    public static Key parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Unreadable key: " + text);
        }
        String[] parts = text.trim().replace('_', ' ').split("\\s+");
        String tonic = parts[0];
        Mode mode = parts.length > 1 && parts[1].toLowerCase().startsWith("min") ? Mode.MINOR : Mode.MAJOR;
        if (parts.length == 1 && tonic.length() > 1 && tonic.endsWith("m") && !tonic.endsWith("dim")) {
            String withoutSuffix = tonic.substring(0, tonic.length() - 1);
            if (parsesAsPitchClass(withoutSuffix)) {
                return new Key(PitchClass.parse(withoutSuffix), Mode.MINOR);
            }
        }
        return new Key(PitchClass.parse(tonic), mode);
    }

    /** The same, for callers that would rather have nothing than an exception. */
    public static Optional<Key> tryParse(String text) {
        try {
            return Optional.of(parse(text));
        } catch (IllegalArgumentException notAKey) {
            return Optional.empty();
        }
    }

    private static boolean parsesAsPitchClass(String text) {
        try {
            PitchClass.parse(text);
            return true;
        } catch (IllegalArgumentException notAPitchClass) {
            return false;
        }
    }

    public Key transpose(Interval interval) {
        return new Key(tonic.transpose(interval), mode);
    }

    public Scale scale() {
        return new Scale(tonic, mode.scaleType());
    }

    /** The scale a chord is built from: minor keys raise the seventh for V and vii. */
    public Scale scale(boolean raisedSeventh) {
        if (mode == Mode.MINOR && raisedSeventh) {
            return new Scale(tonic, ScaleType.HARMONIC_MINOR);
        }
        return scale();
    }

    /**
     * Positive for sharps, negative for flats: D major is 2, F major is -1.
     * Derived from the scale's own accidentals rather than a lookup table.
     */
    public int keySignature() {
        int total = 0;
        for (PitchClass pitchClass : scale().pitchClasses()) {
            total += pitchClass.accidental().offset();
        }
        return total;
    }

    public Chord triad(int degree) {
        return triad(degree, degree == 5 || degree == 7);
    }

    /** Builds a chord by stacking scale thirds on the given degree. */
    public Chord triad(int degree, boolean raisedSeventh) {
        return stackedChord(degree, 3, raisedSeventh);
    }

    public Chord seventh(int degree) {
        return seventh(degree, degree == 5 || degree == 7);
    }

    public Chord seventh(int degree, boolean raisedSeventh) {
        return stackedChord(degree, 4, raisedSeventh);
    }

    private Chord stackedChord(int degree, int noteCount, boolean raisedSeventh) {
        Scale source = scale(raisedSeventh);
        List<PitchClass> classes = source.pitchClasses();
        Set<PitchClass> members = new LinkedHashSet<>();
        for (int i = 0; i < noteCount; i++) {
            members.add(classes.get(Math.floorMod(degree - 1 + i * 2, classes.size())));
        }
        PitchClass root = classes.get(Math.floorMod(degree - 1, classes.size()));
        ChordQuality quality = ChordQuality.identify(root, members)
                .orElseThrow(() -> new IllegalStateException(
                        "No standard quality for degree " + degree + " of " + name() + ": " + members));
        return Chord.of(root, quality);
    }

    public List<Chord> diatonicTriads() {
        return diatonicTriads(mode == Mode.MINOR);
    }

    /** All seven triads. In minor, {@code raisedSeventh} yields the usual V and vii°. */
    public List<Chord> diatonicTriads(boolean raisedSeventh) {
        List<Chord> result = new ArrayList<>(7);
        for (int degree = 1; degree <= 7; degree++) {
            boolean raise = raisedSeventh && (degree == 5 || degree == 7);
            result.add(triad(degree, raise));
        }
        return List.copyOf(result);
    }

    public List<Chord> diatonicSevenths() {
        List<Chord> result = new ArrayList<>(7);
        for (int degree = 1; degree <= 7; degree++) {
            boolean raise = mode == Mode.MINOR && (degree == 5 || degree == 7);
            result.add(seventh(degree, raise));
        }
        return List.copyOf(result);
    }

    public Chord tonicTriad() {
        return triad(1);
    }

    public Chord dominantTriad() {
        return triad(5, true);
    }

    public Chord dominantSeventh() {
        return seventh(5, true);
    }

    public PitchClass leadingTone() {
        return scale(true).degree(7);
    }

    public Optional<Integer> degreeOf(PitchClass pitchClass) {
        Optional<Integer> natural = scale().degreeOf(pitchClass);
        return natural.isPresent() ? natural : scale(true).degreeOf(pitchClass);
    }

    public boolean contains(PitchClass pitchClass) {
        return degreeOf(pitchClass).isPresent();
    }

    public Key relative() {
        return mode == Mode.MAJOR
                ? new Key(tonic.transpose(Interval.MAJOR_SIXTH), Mode.MINOR)
                : new Key(tonic.transpose(Interval.MINOR_THIRD), Mode.MAJOR);
    }

    public Key parallel() {
        return new Key(tonic, mode == Mode.MAJOR ? Mode.MINOR : Mode.MAJOR);
    }

    public String name() {
        return tonic.name() + " " + mode.name().toLowerCase();
    }

    @Override
    public String toString() {
        return name();
    }
}
