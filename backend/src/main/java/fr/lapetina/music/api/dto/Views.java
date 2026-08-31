package fr.lapetina.music.api.dto;

import fr.lapetina.music.tutor.Interaction;
import fr.lapetina.music.tutor.TutorSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read models returned by the API. Entities never leave the service layer. */
public final class Views {

    private Views() {
    }

    public record InteractionView(
            UUID id,
            int sequence,
            String role,
            String content,
            String teachingAction,
            String targetConcept,
            String notationAbc,
            boolean expectsAnswer,
            String answerMode,
            UUID exerciseId,
            Instant createdAt) {

        public static InteractionView of(Interaction interaction) {
            return new InteractionView(
                    interaction.id,
                    interaction.sequence,
                    interaction.role.name(),
                    interaction.content,
                    interaction.teachingAction == null ? null : interaction.teachingAction.name(),
                    interaction.targetConcepts,
                    interaction.notationAbc,
                    interaction.expectsAnswer,
                    interaction.answerMode == null ? null : interaction.answerMode.name(),
                    interaction.exerciseId,
                    interaction.createdAt);
        }
    }

    public record SessionView(
            UUID id,
            UUID learnerId,
            Instant startedAt,
            Instant endedAt,
            List<InteractionView> interactions) {

        public static SessionView of(TutorSession session, List<Interaction> interactions) {
            return new SessionView(session.id, session.learner.id, session.startedAt, session.endedAt,
                    interactions.stream().map(InteractionView::of).toList());
        }
    }

    public record ExerciseView(
            UUID id,
            String conceptId,
            String type,
            String answerMode,
            String prompt,
            String keyContext,
            double difficulty,
            String notationAbc) {

        public static ExerciseView of(fr.lapetina.music.exercise.Exercise exercise) {
            return new ExerciseView(exercise.id, exercise.conceptId, exercise.exerciseType.name(),
                    exercise.answerMode.name(), exercise.prompt, exercise.keyContext,
                    exercise.difficulty, exercise.notationAbc);
        }
    }

    public record EvidenceView(
            UUID id,
            String conceptId,
            String evidenceType,
            String result,
            double difficulty,
            double weight,
            double masteryBefore,
            double masteryAfter,
            String source,
            Instant createdAt) {

        public static EvidenceView of(fr.lapetina.music.learner.Evidence evidence) {
            return new EvidenceView(evidence.id, evidence.conceptId, evidence.evidenceType.name(),
                    evidence.result.name(), evidence.difficulty, evidence.weight,
                    evidence.masteryBefore, evidence.masteryAfter, evidence.source, evidence.createdAt);
        }
    }

    public record ConceptView(
            String id,
            String name,
            String description,
            String category,
            double intrinsicDifficulty,
            List<String> prerequisites,
            List<String> unlocks,
            /** Which practice this belongs to, so the interface can offer a jazz path. */
            String tradition) {
    }

    public record TutorStatusView(
            String narrator,
            boolean languageModelAvailable,
            String model,
            boolean toolsEnabled,
            int conceptCount) {
    }
}
