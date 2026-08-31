package fr.lapetina.music.learner;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything the tutor knows about a learner at one instant. This is the object the
 * teaching policy reads and the prompt is built from.
 */
public record LearnerSnapshot(
        UUID learnerId,
        String displayName,
        List<ConceptMastery> concepts,
        List<ConceptMastery> dueForReview,
        List<MisconceptionView> openMisconceptions,
        Map<String, Double> preferences,
        fr.lapetina.music.exercise.AnswerMode preferredAnswerMode) {

    public ConceptMastery concept(String conceptId) {
        return concepts.stream()
                .filter(concept -> concept.conceptId().equals(conceptId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not in snapshot: " + conceptId));
    }

    public boolean knows(String conceptId) {
        return concepts.stream()
                .filter(concept -> concept.conceptId().equals(conceptId))
                .findFirst()
                .map(ConceptMastery::isKnown)
                .orElse(false);
    }

    public List<ConceptMastery> inProgress() {
        return concepts.stream()
                .filter(concept -> concept.state() != LearningState.UNKNOWN
                        && concept.state() != LearningState.MASTERED)
                .toList();
    }

    public boolean isBlank() {
        return concepts.stream().allMatch(concept -> concept.state() == LearningState.UNKNOWN);
    }
}
