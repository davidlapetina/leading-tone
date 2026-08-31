package fr.lapetina.music.exercise;

import fr.lapetina.music.learner.LearningState;

/** What one answered exercise did to the learner model. */
public record AttemptResult(
        EvaluationOutcome outcome,
        String conceptId,
        double masteryBefore,
        double masteryAfter,
        LearningState state,
        boolean evidenceRecorded) {

    public double delta() {
        return masteryAfter - masteryBefore;
    }
}
