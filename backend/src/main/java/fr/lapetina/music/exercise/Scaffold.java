package fr.lapetina.music.exercise;

import fr.lapetina.music.learner.EvidenceType;

/**
 * How much help the question comes with.
 *
 * <p>A learner who has missed the same concept three times running does not need a fourth
 * identical question; they need a smaller step. The weights make the trade honest — an
 * answer chosen from four options is worth less than one recalled cold, so nobody can farm
 * mastery by taking the easy route.
 */
public enum Scaffold {

    /** Cold recall. What every question starts as. */
    NONE(null),

    /** The same question, with part of the answer given away. */
    HINT(EvidenceType.HINTED_RECALL),

    /** The same question, with a handful of plausible answers to choose between. */
    CHOICES(EvidenceType.MULTIPLE_CHOICE);

    private final EvidenceType evidenceType;

    Scaffold(EvidenceType evidenceType) {
        this.evidenceType = evidenceType;
    }

    /** The evidence a right answer at this level is worth, or null to keep the exercise's own. */
    public EvidenceType evidenceType() {
        return evidenceType;
    }

    /** Consecutive failures on a concept before the tutor makes the step smaller. */
    public static Scaffold forConsecutiveFailures(int failures) {
        if (failures >= 4) {
            return CHOICES;
        }
        return failures >= 2 ? HINT : NONE;
    }
}
