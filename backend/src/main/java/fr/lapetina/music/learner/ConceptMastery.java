package fr.lapetina.music.learner;

import fr.lapetina.music.concept.Concept;
import java.time.Instant;

/** A read model of one concept for one learner, safe to hand to the API and the prompt. */
public record ConceptMastery(
        String conceptId,
        String name,
        String category,
        double mastery,
        double confidence,
        LearningState state,
        int successfulEvidence,
        int failedEvidence,
        int consecutiveFailures,
        Instant lastPracticedAt,
        Instant nextReviewAt) {

    public static ConceptMastery unseen(Concept concept) {
        return new ConceptMastery(concept.id(), concept.name(), concept.category().name(),
                0.0, 0.0, LearningState.UNKNOWN, 0, 0, 0, null, null);
    }

    public static ConceptMastery of(Concept concept, LearnerConcept state) {
        return of(concept, state, state.state);
    }

    /** The state is passed in so it can be derived at read time rather than read from the row. */
    public static ConceptMastery of(Concept concept, LearnerConcept state, LearningState derived) {
        return new ConceptMastery(concept.id(), concept.name(), concept.category().name(),
                state.mastery, state.confidence, derived,
                state.successfulEvidence, state.failedEvidence, state.consecutiveFailures,
                state.lastPracticedAt, state.nextReviewAt);
    }

    public boolean isKnown() {
        return mastery >= MasteryService.LEARNING_THRESHOLD;
    }
}
