package fr.lapetina.music.theory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Chord recipes as spelled intervals above the root. */
public enum ChordQuality {

    MAJOR("", "major triad", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH),
    MINOR("m", "minor triad", Interval.PERFECT_UNISON, Interval.MINOR_THIRD, Interval.PERFECT_FIFTH),
    DIMINISHED("dim", "diminished triad", Interval.PERFECT_UNISON, Interval.MINOR_THIRD, Interval.DIMINISHED_FIFTH),
    AUGMENTED("aug", "augmented triad", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD, Interval.AUGMENTED_FIFTH),
    SUS4("sus4", "suspended fourth", Interval.PERFECT_UNISON, Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH),
    SUS2("sus2", "suspended second", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.PERFECT_FIFTH),

    DOMINANT_SEVENTH("7", "dominant seventh", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH),
    MAJOR_SEVENTH("maj7", "major seventh", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MAJOR_SEVENTH),
    MINOR_SEVENTH("m7", "minor seventh", Interval.PERFECT_UNISON, Interval.MINOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH),
    MINOR_MAJOR_SEVENTH("mMaj7", "minor-major seventh", Interval.PERFECT_UNISON, Interval.MINOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MAJOR_SEVENTH),
    HALF_DIMINISHED_SEVENTH("m7b5", "half-diminished seventh", Interval.PERFECT_UNISON, Interval.MINOR_THIRD,
            Interval.DIMINISHED_FIFTH, Interval.MINOR_SEVENTH),
    DIMINISHED_SEVENTH("dim7", "fully diminished seventh", Interval.PERFECT_UNISON, Interval.MINOR_THIRD,
            Interval.DIMINISHED_FIFTH, Interval.DIMINISHED_SEVENTH),
    AUGMENTED_SEVENTH("7#5", "augmented seventh", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.AUGMENTED_FIFTH, Interval.MINOR_SEVENTH),

    // ---- sixths and extensions: the vocabulary jazz harmony is actually written in ----

    MAJOR_SIXTH("6", "major sixth", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH),
    MINOR_SIXTH("m6", "minor sixth", Interval.PERFECT_UNISON, Interval.MINOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH),

    DOMINANT_NINTH("9", "dominant ninth", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH, Interval.MAJOR_NINTH),
    MAJOR_NINTH("maj9", "major ninth", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MAJOR_SEVENTH, Interval.MAJOR_NINTH),
    MINOR_NINTH("m9", "minor ninth", Interval.PERFECT_UNISON, Interval.MINOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH, Interval.MAJOR_NINTH),

    DOMINANT_ELEVENTH("11", "dominant eleventh", Interval.PERFECT_UNISON, Interval.PERFECT_FIFTH,
            Interval.MINOR_SEVENTH, Interval.MAJOR_NINTH, Interval.PERFECT_ELEVENTH),
    DOMINANT_THIRTEENTH("13", "dominant thirteenth", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.MINOR_SEVENTH, Interval.MAJOR_NINTH, Interval.MAJOR_THIRTEENTH),

    // ---- altered dominants: the tensions that make a V7 lean harder ----

    DOMINANT_FLAT_NINTH("7b9", "dominant seventh flat ninth", Interval.PERFECT_UNISON,
            Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH, Interval.MINOR_NINTH),
    DOMINANT_SHARP_NINTH("7#9", "dominant seventh sharp ninth", Interval.PERFECT_UNISON,
            Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH,
            Interval.AUGMENTED_NINTH),
    DOMINANT_SHARP_ELEVENTH("7#11", "dominant seventh sharp eleventh", Interval.PERFECT_UNISON,
            Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH,
            Interval.AUGMENTED_ELEVENTH),
    DOMINANT_FLAT_THIRTEENTH("7b13", "dominant seventh flat thirteenth", Interval.PERFECT_UNISON,
            Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH,
            Interval.MINOR_THIRTEENTH),

    // Appended deliberately, and new constants must keep being appended. ChordAnalyzer's
    // MIDI identification scans values() in declaration order and takes the first match on
    // size and semitones, and GERMAN_SIXTH is {0,4,7,10} -- exactly a dominant seventh.
    // Declaring it before DOMINANT_SEVENTH would make every G7 come back as an augmented
    // sixth. Spelled identification is unaffected: it compares letters, and Ab C Eb F# is
    // not Ab C Eb Gb.
    ADD_NINE("add9", "Added ninth", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MAJOR_NINTH),

    MINOR_ADD_NINE("madd9", "Minor added ninth", Interval.PERFECT_UNISON, Interval.MINOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MAJOR_NINTH),

    DOMINANT_SEVENTH_SUS4("7sus4", "Dominant seventh suspended fourth", Interval.PERFECT_UNISON,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH),

    SIX_NINE("6/9", "Six-nine", Interval.PERFECT_UNISON, Interval.MAJOR_THIRD,
            Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH, Interval.MAJOR_NINTH),

    MAJOR_SEVENTH_SHARP_ELEVENTH("maj7#11", "Major seventh sharp eleventh", Interval.PERFECT_UNISON,
            Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MAJOR_SEVENTH,
            Interval.AUGMENTED_ELEVENTH),

    ITALIAN_SIXTH("It+6", "Italian augmented sixth", Interval.PERFECT_UNISON,
            Interval.MAJOR_THIRD, Interval.AUGMENTED_SIXTH),

    FRENCH_SIXTH("Fr+6", "French augmented sixth", Interval.PERFECT_UNISON,
            Interval.MAJOR_THIRD, Interval.AUGMENTED_FOURTH, Interval.AUGMENTED_SIXTH),

    GERMAN_SIXTH("Ger+6", "German augmented sixth", Interval.PERFECT_UNISON,
            Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.AUGMENTED_SIXTH);

    private final String symbol;
    private final String displayName;
    private final List<Interval> intervals;

    ChordQuality(String symbol, String displayName, Interval... intervals) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.intervals = List.of(intervals);
    }

    public String symbol() {
        return symbol;
    }

    public String displayName() {
        return displayName;
    }

    public List<Interval> intervals() {
        return intervals;
    }

    public int size() {
        return intervals.size();
    }

    public boolean isSeventh() {
        return intervals.size() == 4;
    }

    /** True for ninths and beyond, where a tension sits above the seventh. */
    public boolean isExtended() {
        return intervals.stream().anyMatch(interval -> interval.number() > 7);
    }

    /** True when a tension has been raised or lowered — the altered dominants. */
    public boolean isAltered() {
        return switch (this) {
            case DOMINANT_FLAT_NINTH, DOMINANT_SHARP_NINTH, DOMINANT_SHARP_ELEVENTH,
                 DOMINANT_FLAT_THIRTEENTH, AUGMENTED_SEVENTH -> true;
            default -> false;
        };
    }

    public boolean isAugmentedSixth() {
        return this == ITALIAN_SIXTH || this == FRENCH_SIXTH || this == GERMAN_SIXTH;
    }

    public boolean isTriad() {
        return intervals.size() == 3;
    }

    /** True for qualities whose third and seventh pull towards a resolution. */
    public boolean isDominantFunctioning() {
        return this == DOMINANT_SEVENTH || this == DIMINISHED_SEVENTH || this == HALF_DIMINISHED_SEVENTH
                || this == DOMINANT_NINTH || this == DOMINANT_ELEVENTH || this == DOMINANT_THIRTEENTH
                || isAltered();
    }

    public Set<Integer> semitonesAboveRoot() {
        Set<Integer> result = new LinkedHashSet<>();
        for (Interval interval : intervals) {
            result.add(Math.floorMod(interval.semitones(), 12));
        }
        return result;
    }

    /** Identifies a quality from a spelled member set, which is how diatonic chords get named. */
    public static Optional<ChordQuality> identify(PitchClass root, Set<PitchClass> members) {
        for (ChordQuality quality : values()) {
            Set<PitchClass> candidate = new LinkedHashSet<>();
            for (Interval interval : quality.intervals) {
                candidate.add(root.transpose(interval));
            }
            if (candidate.equals(members)) {
                return Optional.of(quality);
            }
        }
        return Optional.empty();
    }

    /**
     * Reads a chord quality suffix.
     *
     * <p>Exact matches come before case-insensitive ones, and that ordering is the whole
     * point: {@code M7} means a major seventh and {@code m7} a minor one, so a
     * case-insensitive scan run first would quietly turn every {@code CM7} into C minor
     * seventh. It used to.
     */
    public static Optional<ChordQuality> parseSymbol(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }
        String text = symbol.trim();
        for (ChordQuality quality : values()) {
            if (quality.symbol.equals(text)) {
                return Optional.of(quality);
            }
        }
        Optional<ChordQuality> alias = alias(text);
        if (alias.isPresent()) {
            return alias;
        }
        for (ChordQuality quality : values()) {
            if (quality.symbol.equalsIgnoreCase(text)) {
                return Optional.of(quality);
            }
        }
        return alias(text.toLowerCase(java.util.Locale.ROOT));
    }

    private static Optional<ChordQuality> alias(String symbol) {
        return switch (symbol) {
            case "M", "maj", "Maj", "ma" -> Optional.of(MAJOR);
            case "min", "-", "mi" -> Optional.of(MINOR);
            case "o", "°", "dim" -> Optional.of(DIMINISHED);
            case "+", "aug" -> Optional.of(AUGMENTED);
            case "ø", "ø7", "m7-5", "mi7b5", "-7b5" -> Optional.of(HALF_DIMINISHED_SEVENTH);
            case "o7", "°7", "dim7" -> Optional.of(DIMINISHED_SEVENTH);
            case "M7", "Maj7", "Δ", "Δ7", "ma7" -> Optional.of(MAJOR_SEVENTH);
            case "min7", "-7", "mi7" -> Optional.of(MINOR_SEVENTH);
            case "mM7", "m/maj7", "minMaj7", "-M7" -> Optional.of(MINOR_MAJOR_SEVENTH);
            case "aug7", "+7", "7+5" -> Optional.of(AUGMENTED_SEVENTH);
            case "sus" -> Optional.of(SUS4);
            case "add2" -> Optional.of(ADD_NINE);
            case "69", "6add9" -> Optional.of(SIX_NINE);
            case "7sus" -> Optional.of(DOMINANT_SEVENTH_SUS4);
            case "It6", "It+6" -> Optional.of(ITALIAN_SIXTH);
            case "Fr6", "Fr+6" -> Optional.of(FRENCH_SIXTH);
            case "Ger6", "Ger+6" -> Optional.of(GERMAN_SIXTH);
            default -> Optional.empty();
        };
    }
}
