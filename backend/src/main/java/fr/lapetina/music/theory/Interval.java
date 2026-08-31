package fr.lapetina.music.theory;

import java.util.Objects;

/**
 * A spelled interval: a diatonic {@code number} (1 = unison, 2 = second, ...) plus a
 * quality. An augmented fourth and a diminished fifth are both six semitones but are
 * different intervals, and the tutor needs to be able to say so.
 */
public record Interval(int number, IntervalQuality quality) {

    private static final int[] BASE_SEMITONES = {0, 2, 4, 5, 7, 9, 11};

    public static final Interval PERFECT_UNISON = new Interval(1, IntervalQuality.PERFECT);
    public static final Interval MINOR_SECOND = new Interval(2, IntervalQuality.MINOR);
    public static final Interval MAJOR_SECOND = new Interval(2, IntervalQuality.MAJOR);
    public static final Interval AUGMENTED_SECOND = new Interval(2, IntervalQuality.AUGMENTED);
    public static final Interval MINOR_THIRD = new Interval(3, IntervalQuality.MINOR);
    public static final Interval MAJOR_THIRD = new Interval(3, IntervalQuality.MAJOR);
    public static final Interval PERFECT_FOURTH = new Interval(4, IntervalQuality.PERFECT);
    public static final Interval AUGMENTED_FOURTH = new Interval(4, IntervalQuality.AUGMENTED);
    public static final Interval DIMINISHED_FIFTH = new Interval(5, IntervalQuality.DIMINISHED);
    public static final Interval PERFECT_FIFTH = new Interval(5, IntervalQuality.PERFECT);
    public static final Interval AUGMENTED_FIFTH = new Interval(5, IntervalQuality.AUGMENTED);
    public static final Interval MINOR_SIXTH = new Interval(6, IntervalQuality.MINOR);
    public static final Interval MAJOR_SIXTH = new Interval(6, IntervalQuality.MAJOR);
    public static final Interval DIMINISHED_SEVENTH = new Interval(7, IntervalQuality.DIMINISHED);
    public static final Interval MINOR_SEVENTH = new Interval(7, IntervalQuality.MINOR);
    public static final Interval MAJOR_SEVENTH = new Interval(7, IntervalQuality.MAJOR);
    public static final Interval PERFECT_OCTAVE = new Interval(8, IntervalQuality.PERFECT);

    public Interval {
        if (number < 1) {
            throw new IllegalArgumentException("Interval number must be >= 1, was " + number);
        }
        Objects.requireNonNull(quality, "quality");
        if (isPerfectFamily(number) && (quality == IntervalQuality.MAJOR || quality == IntervalQuality.MINOR)) {
            throw new IllegalArgumentException(quality + " is not a valid quality for interval number " + number);
        }
        if (!isPerfectFamily(number) && quality == IntervalQuality.PERFECT) {
            throw new IllegalArgumentException("PERFECT is not a valid quality for interval number " + number);
        }
    }

    /** True for unisons, fourths, fifths and octaves, which take perfect rather than major/minor. */
    public static boolean isPerfectFamily(int number) {
        int simple = simpleNumber(number);
        return simple == 1 || simple == 4 || simple == 5;
    }

    private static int simpleNumber(int number) {
        return ((number - 1) % 7) + 1;
    }

    public int semitones() {
        int octaves = (number - 1) / 7;
        int base = BASE_SEMITONES[simpleNumber(number) - 1];
        return octaves * 12 + base + qualityOffset();
    }

    private int qualityOffset() {
        boolean perfectFamily = isPerfectFamily(number);
        return switch (quality) {
            case DIMINISHED -> perfectFamily ? -1 : -2;
            case MINOR -> -1;
            case PERFECT, MAJOR -> 0;
            case AUGMENTED -> 1;
        };
    }

    /** Diatonic distance in letter steps: a third moves two letters. */
    public int diatonicSteps() {
        return number - 1;
    }

    public String symbol() {
        return quality.symbol() + number;
    }

    /**
     * The ascending simple interval from {@code from} to {@code to}, spelled from the
     * letter names rather than from semitone distance alone.
     */
    public static Interval between(PitchClass from, PitchClass to) {
        int letterSteps = Math.floorMod(to.letter().diatonicIndex() - from.letter().diatonicIndex(), 7);
        int number = letterSteps + 1;
        int semitoneDistance = Math.floorMod(to.semitone() - from.semitone(), 12);
        int base = BASE_SEMITONES[letterSteps];
        int delta = semitoneDistance - base;
        if (delta > 6) {
            delta -= 12;
        } else if (delta < -6) {
            delta += 12;
        }
        return new Interval(number, qualityForOffset(number, delta));
    }

    private static IntervalQuality qualityForOffset(int number, int offset) {
        if (isPerfectFamily(number)) {
            return switch (offset) {
                case -1 -> IntervalQuality.DIMINISHED;
                case 0 -> IntervalQuality.PERFECT;
                case 1 -> IntervalQuality.AUGMENTED;
                default -> throw new IllegalArgumentException(
                        "No standard quality for a " + number + " altered by " + offset + " semitones");
            };
        }
        return switch (offset) {
            case -2 -> IntervalQuality.DIMINISHED;
            case -1 -> IntervalQuality.MINOR;
            case 0 -> IntervalQuality.MAJOR;
            case 1 -> IntervalQuality.AUGMENTED;
            default -> throw new IllegalArgumentException(
                    "No standard quality for a " + number + " altered by " + offset + " semitones");
        };
    }

    public static Interval parse(String text) {
        String trimmed = text.trim();
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException("Unparseable interval: " + text);
        }
        IntervalQuality quality = switch (trimmed.charAt(0)) {
            case 'd' -> IntervalQuality.DIMINISHED;
            case 'm' -> IntervalQuality.MINOR;
            case 'P', 'p' -> IntervalQuality.PERFECT;
            case 'M' -> IntervalQuality.MAJOR;
            case 'A' -> IntervalQuality.AUGMENTED;
            default -> throw new IllegalArgumentException("Unknown interval quality in: " + text);
        };
        return new Interval(Integer.parseInt(trimmed.substring(1)), quality);
    }

    @Override
    public String toString() {
        return symbol();
    }
}
