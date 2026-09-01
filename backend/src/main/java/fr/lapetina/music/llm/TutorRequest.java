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
        String modelDirective,
        /**
         * What was computed, quoted and cited for this turn. Gathered by the orchestrator
         * before the model is called, so the model explains evidence rather than supplying
         * it.
         */
        fr.lapetina.music.knowledge.router.TutorKnowledge knowledge,
        /**
         * Whether the previous answer was right, or null when there was no answer. The model
         * is told this in its directive and mostly obeys it; this is what lets the turn it
         * produces be checked against it.
         */
        Boolean answeredCorrectly) {

    public TutorRequest {
        knowledge = knowledge == null
                ? fr.lapetina.music.knowledge.router.TutorKnowledge.EMPTY : knowledge;
    }

    public TutorRequest(java.util.UUID sessionId, LearnerSnapshot snapshot, TeachingDecision decision,
                        Exercise exercise, String learnerMessage, String evaluationFeedback,
                        String modelDirective) {
        this(sessionId, snapshot, decision, exercise, learnerMessage, evaluationFeedback,
                modelDirective, fr.lapetina.music.knowledge.router.TutorKnowledge.EMPTY, null);
    }

    public TutorRequest(java.util.UUID sessionId, LearnerSnapshot snapshot, TeachingDecision decision,
                        Exercise exercise, String learnerMessage, String evaluationFeedback) {
        this(sessionId, snapshot, decision, exercise, learnerMessage, evaluationFeedback, null);
    }
}
