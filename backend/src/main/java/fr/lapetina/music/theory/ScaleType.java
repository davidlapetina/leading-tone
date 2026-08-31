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
            Interval.PERFECT_FOURTH, Interval.DIMINISHED_FIFTH, Interval.MINOR_SIXTH, Interval.MINOR_SEVENTH),

    // Jazz and twentieth-century scales. Spelling still comes from the intervals, so the
    // altered scale keeps its diminished fourth: G altered contains C flat, not B.
    MAJOR_PENTATONIC("Major pentatonic", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND,
            Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH),

    MINOR_PENTATONIC("Minor pentatonic", Interval.PERFECT_UNISON, Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH),

    BLUES("Blues", Interval.PERFECT_UNISON, Interval.MINOR_THIRD, Interval.PERFECT_FOURTH,
            Interval.DIMINISHED_FIFTH, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH),

    WHOLE_TONE("Whole tone", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND, Interval.MAJOR_THIRD,
            Interval.AUGMENTED_FOURTH, Interval.AUGMENTED_FIFTH, Interval.AUGMENTED_SIXTH),

    OCTATONIC_HALF_WHOLE("Octatonic (half-whole)", Interval.PERFECT_UNISON, Interval.MINOR_SECOND,
            Interval.MINOR_THIRD, Interval.MAJOR_THIRD, Interval.AUGMENTED_FOURTH,
            Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH, Interval.MINOR_SEVENTH),

    OCTATONIC_WHOLE_HALF("Octatonic (whole-half)", Interval.PERFECT_UNISON, Interval.MAJOR_SECOND,
            Interval.MINOR_THIRD, Interval.PERFECT_FOURTH, Interval.DIMINISHED_FIFTH,
            Interval.MINOR_SIXTH, Interval.MAJOR_SIXTH, Interval.MAJOR_SEVENTH),

    ALTERED("Altered", Interval.PERFECT_UNISON, Interval.MINOR_SECOND, Interval.MINOR_THIRD,
            Interval.DIMINISHED_FOURTH, Interval.DIMINISHED_FIFTH, Interval.MINOR_SIXTH,
            Interval.MINOR_SEVENTH);

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

    /**
     * Whether this scale has seven degrees. Scale-degree names, key signatures and the
     * diatonic chord stack all assume seven, so anything that reaches them must check.
     */
    public boolean isHeptatonic() {
        return pattern.size() == 7;
    }

    public int degreeCount() {
        return pattern.size();
    }
}
