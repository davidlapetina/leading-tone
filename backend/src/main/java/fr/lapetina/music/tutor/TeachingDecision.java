package fr.lapetina.music.tutor;

import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.learner.MisconceptionView;
import java.util.List;

/**
 * The pedagogical instruction handed to the language model.
 *
 * @param supportingConcepts already-mastered concepts worth anchoring the explanation to
 * @param rationale          why the policy chose this, recorded so decisions stay auditable
 * @param learnerAskedAbout  the concept the learner raised themselves, when they raised one
 */
public record TeachingDecision(
        TeachingAction action,
        String conceptId,
        String conceptName,
        List<String> supportingConcepts,
        double difficulty,
        AnswerMode preferredAnswerMode,
        String rationale,
        MisconceptionView misconception,
        String learnerAskedAbout) {

    public TeachingDecision(TeachingAction action, String conceptId, String conceptName,
                            List<String> supportingConcepts, double difficulty,
                            AnswerMode preferredAnswerMode, String rationale,
                            MisconceptionView misconception) {
        this(action, conceptId, conceptName, supportingConcepts, difficulty, preferredAnswerMode,
                rationale, misconception, null);
    }

    public TeachingDecision answering(String askedAboutConceptId) {
        return new TeachingDecision(action, conceptId, conceptName, supportingConcepts, difficulty,
                preferredAnswerMode, rationale, misconception, askedAboutConceptId);
    }

    /**
     * Every action ends in something to do, so this depends only on whether there is a
     * channel to answer on.
     */
    public boolean expectsAnswer() {
        return preferredAnswerMode != AnswerMode.NONE;
    }
}
