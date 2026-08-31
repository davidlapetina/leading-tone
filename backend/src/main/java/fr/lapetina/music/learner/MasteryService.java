package fr.lapetina.music.learner;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;

/**
 * The mastery arithmetic, deliberately explicit rather than Bayesian for V1.
 *
 * <p>Every update is a function of four things: whether the answer was right, how hard
 * the question was, what kind of evidence it is, and how sure the evaluator is. Gains
 * approach 1 asymptotically so no single answer can declare a concept learned, and
 * losses are multiplicative so a wrong answer costs more when mastery was high.
 *
 * <p>Nothing here consults a language model, and no language model can call it.
 */
@ApplicationScoped
public class MasteryService {

    /** How fast mastery moves per unit of weighted evidence. */
    static final double LEARNING_RATE = 0.30;

    /** Mistakes are weighted slightly heavier than successes, because they are more informative. */
    static final double MISTAKE_MULTIPLIER = 1.2;

    /** Skipping a question is weak evidence of not knowing, not proof of it. */
    static final double SKIP_MULTIPLIER = 0.25;

    static final double INTRODUCED_THRESHOLD = 0.15;
    static final double LEARNING_THRESHOLD = 0.45;
    static final double PRACTICING_THRESHOLD = 0.70;
    static final double RELIABLE_THRESHOLD = 0.88;

    /** Mastery is not claimed without repeated high-quality evidence, whatever the number says. */
    static final double MASTERY_CONFIDENCE_THRESHOLD = 0.80;
    static final int MASTERY_STRONG_EVIDENCE_REQUIRED = 2;

    /**
     * Folds one observation into the learner's state, mutating {@code target} and
     * returning what changed.
     */
    public MasteryUpdate apply(LearnerConcept target, EvidenceType type, EvidenceResult result,
                              double difficulty, double observationConfidence, Instant now) {
        double masteryBefore = target.mastery;
        LearningState stateBefore = target.state;
        double weight = weightOf(type, difficulty, observationConfidence);

        if (result.isPositive()) {
            double gain = weight * LEARNING_RATE * result.correctness();
            target.mastery = masteryBefore + (1.0 - masteryBefore) * gain;
            target.successfulEvidence++;
            target.consecutiveFailures = 0;
            if (type.isStrong() && result == EvidenceResult.CORRECT) {
                target.strongEvidence++;
            }
        } else {
            double multiplier = result == EvidenceResult.SKIPPED ? SKIP_MULTIPLIER : MISTAKE_MULTIPLIER;
            double loss = weight * LEARNING_RATE * multiplier;
            target.mastery = masteryBefore * (1.0 - Math.min(loss, 0.9));
            target.failedEvidence++;
            if (result != EvidenceResult.SKIPPED) {
                target.consecutiveFailures++;
            }
        }

        target.mastery = clamp(target.mastery);
        target.confidence = confidenceFor(target.totalEvidence());
        target.lastPracticedAt = now;
        target.state = deriveState(target, now);
        return new MasteryUpdate(masteryBefore, target.mastery, target.confidence, weight, stateBefore, target.state);
    }

    /**
     * The multiplier applied to one observation. Difficulty is folded in on a half-scale
     * so that an easy question still counts for something.
     */
    public double weightOf(EvidenceType type, double difficulty, double observationConfidence) {
        double normalizedDifficulty = clamp(difficulty);
        double normalizedConfidence = clamp(observationConfidence);
        return type.weight() * normalizedConfidence * (0.5 + 0.5 * normalizedDifficulty);
    }

    /** Confidence in the estimate itself: it rises with the amount of evidence, never with a single answer. */
    public double confidenceFor(int totalEvidence) {
        return clamp(1.0 - Math.pow(0.75, totalEvidence));
    }

    /**
     * The label for a concept. MASTERED additionally requires that the learner has
     * actually demonstrated the concept — played it, explained it, or transferred it —
     * more than once.
     */
    public LearningState deriveState(LearnerConcept target, Instant now) {
        if (target.totalEvidence() == 0) {
            return LearningState.UNKNOWN;
        }
        if (target.isDue(now) && target.mastery >= LEARNING_THRESHOLD) {
            return LearningState.NEEDS_REVIEW;
        }
        if (target.mastery < INTRODUCED_THRESHOLD) {
            return LearningState.INTRODUCED;
        }
        if (target.mastery < LEARNING_THRESHOLD) {
            return LearningState.LEARNING;
        }
        if (target.mastery < PRACTICING_THRESHOLD) {
            return LearningState.PRACTICING;
        }
        if (target.mastery < RELIABLE_THRESHOLD) {
            return LearningState.RELIABLE;
        }
        boolean earned = target.confidence >= MASTERY_CONFIDENCE_THRESHOLD
                && target.strongEvidence >= MASTERY_STRONG_EVIDENCE_REQUIRED;
        return earned ? LearningState.MASTERED : LearningState.RELIABLE;
    }


    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
