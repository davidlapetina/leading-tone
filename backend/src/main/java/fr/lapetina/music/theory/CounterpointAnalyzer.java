package fr.lapetina.music.theory;

import java.util.List;

/**
 * Two voices, and what they are doing to each other.
 *
 * <p>Species counterpoint is largely a set of rules about motion and about which intervals
 * may be approached how. Both are computable, so the tutor can mark them rather than
 * opine about them: parallel fifths are a fact, not a judgement.
 */
public final class CounterpointAnalyzer {

    private CounterpointAnalyzer() {
    }

    /** How the two voices moved from one pair of notes to the next. */
    public static Motion motionBetween(Note lowerFrom, Note upperFrom, Note lowerTo, Note upperTo) {
        int lower = Integer.compare(lowerTo.midi(), lowerFrom.midi());
        int upper = Integer.compare(upperTo.midi(), upperFrom.midi());

        if (lower == 0 && upper == 0) {
            return Motion.STATIC;
        }
        if (lower == 0 || upper == 0) {
            return Motion.OBLIQUE;
        }
        if (lower != upper) {
            return Motion.CONTRARY;
        }
        boolean sameInterval = upperFrom.midi() - lowerFrom.midi() == upperTo.midi() - lowerTo.midi();
        return sameInterval ? Motion.PARALLEL : Motion.SIMILAR;
    }

    /** The interval between two sounding voices, spelled. */
    public static Interval intervalBetween(Note lower, Note upper) {
        return Interval.between(lower.pitchClass(), upper.pitchClass());
    }

    /** True for the perfect consonances that must not be reached in parallel. */
    public static boolean isPerfectConsonance(Interval interval) {
        int simple = ((interval.number() - 1) % 7) + 1;
        return interval.quality() == IntervalQuality.PERFECT && (simple == 1 || simple == 5);
    }

    /**
     * The forbidden parallel: two voices moving the same way from one perfect fifth or
     * octave straight into another. It is the first rule anyone learns and the first one
     * anyone breaks.
     */
    public static boolean hasParallelPerfects(Note lowerFrom, Note upperFrom, Note lowerTo, Note upperTo) {
        if (motionBetween(lowerFrom, upperFrom, lowerTo, upperTo) != Motion.PARALLEL) {
            return false;
        }
        return isPerfectConsonance(intervalBetween(lowerFrom, upperFrom))
                && isPerfectConsonance(intervalBetween(lowerTo, upperTo));
    }

    /** Consonant intervals in first-species counterpoint: thirds, sixths, fifths and octaves. */
    public static boolean isConsonant(Interval interval) {
        int simple = ((interval.number() - 1) % 7) + 1;
        return switch (simple) {
            case 1, 5 -> interval.quality() == IntervalQuality.PERFECT;
            case 3, 6 -> interval.quality() == IntervalQuality.MAJOR
                    || interval.quality() == IntervalQuality.MINOR;
            default -> false;
        };
    }

    /** Scans a two-voice passage and reports the first bar where parallel perfects appear. */
    public static int firstParallelPerfect(List<Note> lower, List<Note> upper) {
        int pairs = Math.min(lower.size(), upper.size());
        for (int i = 1; i < pairs; i++) {
            if (hasParallelPerfects(lower.get(i - 1), upper.get(i - 1), lower.get(i), upper.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
