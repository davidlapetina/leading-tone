package fr.lapetina.music.learner;

import java.util.UUID;

/**
 * A proposed observation, before it has been accepted into the learner model.
 *
 * <p>The language model may produce these; only {@link EvidenceService} decides whether
 * they are recorded, and it is the only thing that can change mastery.
 */
public record EvidenceObservation(
        String conceptId,
        EvidenceType type,
        EvidenceResult result,
        double difficulty,
        double confidence,
        String source,
        UUID sessionId,
        UUID interactionId,
        UUID exerciseId) {

    public static EvidenceObservation of(String conceptId, EvidenceType type, EvidenceResult result,
                                         double difficulty, String source) {
        return new EvidenceObservation(conceptId, type, result, difficulty, 1.0, source, null, null, null);
    }

    public EvidenceObservation inSession(UUID session, UUID interaction) {
        return new EvidenceObservation(conceptId, type, result, difficulty, confidence, source,
                session, interaction, exerciseId);
    }

    public EvidenceObservation forExercise(UUID exercise) {
        return new EvidenceObservation(conceptId, type, result, difficulty, confidence, source,
                sessionId, interactionId, exercise);
    }

    public EvidenceObservation withConfidence(double newConfidence) {
        return new EvidenceObservation(conceptId, type, result, difficulty, newConfidence, source,
                sessionId, interactionId, exerciseId);
    }
}
