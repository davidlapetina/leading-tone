package fr.lapetina.music.theory;

import java.util.List;

/** Scale patterns expressed as spelled intervals from the tonic, so spelling survives. */
public enum ScaleType {

    MAJOR("Major", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.MAJOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH, Interval.MAJOR_SEVENTH),

    NATURAL_MINOR("Natural minor", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MINOR_SIXTH, Interval.MINOR_SEVENTH),

    HARMONIC_MINOR("Harmonic minor", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MINOR_SIXTH, Interval.MAJOR_SEVENTH),

    MELODIC_MINOR("Melodic minor", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH, Interval.MAJOR_SEVENTH),

    DORIAN("Dorian", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH, Interval.MINOR_SEVENTH),

    PHRYGIAN("Phrygian", Interval.PERFECT_UNISON, Interval.MINOR_SECOND, Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MINOR_SIXTH, Interval.MINOR_SEVENTH),

    LYDIAN("Lydian", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.MAJOR_THIRD,
            Interval.AUGMENTED_FOURTH, Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH, Interval.MAJOR_SEVENTH),

    MIXOLYDIAN("Mixolydian", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.MAJOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH, Interval.MINOR_SEVENTH),

    LOCRIAN("Locrian", Interval.PERFECT_UNISON, Interval.MINOR_SECOND, Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.DIMINISHED_FIFTH, Interval.MINOR_SIXTH, Interval.MINOR_SEVENTH);

    private final String displayName;
    private final List<Interval> pattern;

    ScaleType(String displayName, Interval... pattern) {
        this.displayName = displayName;
        this.pattern = List.of(pattern);
    }

    public String displayName() {
        return displayName;
    }

    public List<Interval> pattern() {
        return pattern;
    }

    public int degreeCount() {
        return pattern.size();
    }
}
