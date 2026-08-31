package fr.lapetina.music.llm;

import fr.lapetina.music.exercise.Exercise;
import fr.lapetina.music.learner.LearnerSnapshot;
import fr.lapetina.music.tutor.TeachingDecision;
import java.util.UUID;

/**
 * Everything the language model is given for one turn.
 *
 * <p>There is no conversation history here: the model keeps its own, keyed by session,
 * and the durable record of what was said is the {@code interaction} table.
 *
 * @param sessionId scopes the model's conversation memory to this session
 * @param exercise  the deterministically generated exercise to put to the learner, or null
 */
public record TutorRequest(
        UUID sessionId,
        LearnerSnapshot snapshot,
        TeachingDecision decision,
        Exercise exercise,
        String learnerMessage,
        /** Short, learner-facing: "Expected Eb." Shown as-is when there is no model. */
        String evaluationFeedback,
        /** Instructions about that verdict, for the model only. Never shown to anyone. */
        String modelDirective) {

    public TutorRequest(java.util.UUID sessionId, LearnerSnapshot snapshot, TeachingDecision decision,
                        Exercise exercise, String learnerMessage, String evaluationFeedback) {
        this(sessionId, snapshot, decision, exercise, learnerMessage, evaluationFeedback, null);
    }
}
