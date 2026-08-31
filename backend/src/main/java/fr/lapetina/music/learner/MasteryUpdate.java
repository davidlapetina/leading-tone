package fr.lapetina.music.learner;

/** The result of folding one piece of evidence into a learner's model of a concept. */
public record MasteryUpdate(
        double masteryBefore,
        double masteryAfter,
        double confidence,
        double weight,
        LearningState stateBefore,
        LearningState stateAfter) {

    public double delta() {
        return masteryAfter - masteryBefore;
    }

    public boolean changedState() {
        return stateBefore != stateAfter;
    }
}
