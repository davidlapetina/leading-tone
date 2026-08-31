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
            Interval.AUGMENTED_FIFTH, Interval.MINOR_SEVENTH);

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

    public boolean isTriad() {
        return intervals.size() == 3;
    }

    /** True for qualities whose third and seventh pull towards a resolution. */
    public boolean isDominantFunctioning() {
        return this == DOMINANT_SEVENTH || this == DIMINISHED_SEVENTH || this == HALF_DIMINISHED_SEVENTH;
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

    public static Optional<ChordQuality> parseSymbol(String symbol) {
        for (ChordQuality quality : values()) {
            if (quality.symbol.equalsIgnoreCase(symbol)) {
                return Optional.of(quality);
            }
        }
        return switch (symbol) {
            case "M", "maj" -> Optional.of(MAJOR);
            case "min", "-" -> Optional.of(MINOR);
            case "o", "°" -> Optional.of(DIMINISHED);
            case "+" -> Optional.of(AUGMENTED);
            case "ø", "ø7", "m7-5" -> Optional.of(HALF_DIMINISHED_SEVENTH);
            case "o7", "°7" -> Optional.of(DIMINISHED_SEVENTH);
            case "M7", "Maj7", "Δ" -> Optional.of(MAJOR_SEVENTH);
            case "min7", "-7" -> Optional.of(MINOR_SEVENTH);
            default -> Optional.empty();
        };
    }
}
