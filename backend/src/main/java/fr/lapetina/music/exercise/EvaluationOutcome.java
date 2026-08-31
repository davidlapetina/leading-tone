package fr.lapetina.music.exercise;

import fr.lapetina.music.learner.EvidenceResult;

/**
 * The verdict on one attempt.
 *
 * @param confidence            how sure the evaluator is; 1.0 for deterministic checks
 * @param requiresModelJudgement true when the answer is free prose that no deterministic
 *                               rule can grade, in which case the model may propose a
 *                               verdict and it is recorded at reduced confidence
 */
public record EvaluationOutcome(
        EvidenceResult result,
        String feedback,
        String detail,
        String misconceptionCode,
        String misconceptionDescription,
        double confidence,
        boolean requiresModelJudgement) {

    public static EvaluationOutcome correct(String feedback) {
        return new EvaluationOutcome(EvidenceResult.CORRECT, feedback, null, null, null, 1.0, false);
    }

    public static EvaluationOutcome incorrect(String feedback) {
        return new EvaluationOutcome(EvidenceResult.INCORRECT, feedback, null, null, null, 1.0, false);
    }

    public static EvaluationOutcome partial(String feedback) {
        return new EvaluationOutcome(EvidenceResult.PARTIALLY_CORRECT, feedback, null, null, null, 1.0, false);
    }

    public static EvaluationOutcome needsJudgement(String feedback) {
        return new EvaluationOutcome(EvidenceResult.PARTIALLY_CORRECT, feedback, null, null, null, 0.0, true);
    }

    public EvaluationOutcome withMisconception(String code, String description) {
        return new EvaluationOutcome(result, feedback, detail, code, description, confidence, requiresModelJudgement);
    }

    public boolean isCorrect() {
        return result == EvidenceResult.CORRECT;
    }
}
