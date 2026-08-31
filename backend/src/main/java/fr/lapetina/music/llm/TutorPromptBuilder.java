package fr.lapetina.music.llm;

import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import fr.lapetina.music.learner.ConceptMastery;
import fr.lapetina.music.learner.LearnerSnapshot;
import fr.lapetina.music.learner.LearningState;
import fr.lapetina.music.learner.MisconceptionView;
import fr.lapetina.music.tutor.TeachingDecision;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns the learner model and the policy's decision into the text the model is given.
 *
 * <p>The prompt states what to do. It never asks the model what to do next, and it never
 * offers it a way to change the record.
 */
@ApplicationScoped
public class TutorPromptBuilder {

    private static final int MAX_LISTED_CONCEPTS = 12;

    @Inject
    ConceptGraph conceptGraph;

    @Inject
    TheoryBriefing theoryBriefing;

    public String learnerState(LearnerSnapshot snapshot) {
        StringBuilder builder = new StringBuilder("Learner: ").append(snapshot.displayName()).append('\n');

        List<ConceptMastery> working = snapshot.inProgress().stream().limit(MAX_LISTED_CONCEPTS).toList();
        if (working.isEmpty()) {
            builder.append("Nothing has been observed yet. This is the first conversation.\n");
        } else {
            builder.append("Currently holding:\n");
            for (ConceptMastery concept : working) {
                builder.append("- %s (%s): mastery %.2f, confidence %.2f, %s%n".formatted(
                        concept.conceptId(), concept.name(), concept.mastery(), concept.confidence(),
                        concept.state()));
            }
        }

        String mastered = snapshot.concepts().stream()
                .filter(concept -> concept.state() == LearningState.MASTERED)
                .map(ConceptMastery::conceptId)
                .collect(Collectors.joining(", "));
        if (!mastered.isEmpty()) {
            builder.append("Solid: ").append(mastered).append('\n');
        }

        if (!snapshot.dueForReview().isEmpty()) {
            builder.append("Due for review: ")
                    .append(snapshot.dueForReview().stream()
                            .map(ConceptMastery::conceptId)
                            .collect(Collectors.joining(", ")))
                    .append('\n');
        }

        if (!snapshot.openMisconceptions().isEmpty()) {
            builder.append("Observed mistakes:\n");
            for (MisconceptionView misconception : snapshot.openMisconceptions()) {
                builder.append("- %s (seen %d times): %s%n".formatted(
                        misconception.code(), misconception.occurrences(), misconception.description()));
            }
        }

        if (snapshot.preferredAnswerMode() != null) {
            builder.append("Has asked to practise by ")
                    .append(snapshot.preferredAnswerMode() == fr.lapetina.music.exercise.AnswerMode.MIDI
                            ? "playing at the keyboard" : "writing")
                    .append(", so honour that.\n");
        }
        builder.append("Answers best through: ")
                .append(snapshot.preferences().getOrDefault("keyboardPreference", 0.5) >= 0.5
                        ? "playing at the keyboard" : "writing")
                .append('\n');
        return builder.toString();
    }

    public String instruction(TeachingDecision decision) {
        Concept concept = conceptGraph.require(decision.conceptId());
        StringBuilder builder = new StringBuilder();
        builder.append("Pedagogical action: ").append(decision.action()).append('\n');
        builder.append("Target concept: ").append(concept.id()).append(" — ").append(concept.name()).append('\n');
        builder.append("What it is: ").append(concept.description()).append('\n');
        builder.append("Why now: ").append(decision.rationale()).append('\n');
        if (!decision.supportingConcepts().isEmpty()) {
            builder.append("Anchor the explanation to what is already solid: ")
                    .append(String.join(", ", decision.supportingConcepts())).append('\n');
        }
        if (decision.misconception() != null) {
            builder.append("Mistake to address: ").append(decision.misconception().description()).append('\n');
        }
        if (decision.learnerAskedAbout() != null) {
            builder.append("The learner asked about ").append(decision.learnerAskedAbout())
                    .append(". Answer that first, in a sentence or two");
            if (!decision.learnerAskedAbout().equals(decision.conceptId())) {
                builder.append(", then say plainly that ").append(decision.conceptId())
                        .append(" has to come first and turn to it");
            }
            builder.append(".\n");
        }
        builder.append(decision.action().explainsBeforeAsking()
                ? "This concept is new or unsettled: explain before you ask, and take your time over it.\n"
                : "Lead into the question with a sentence or two that connects it to what they "
                        + "already know, then ask it.\n");
        builder.append("Difficulty aimed at: %.2f%n".formatted(decision.difficulty()));

        // Facts first, so the model has no reason to invent any.
        String briefing = theoryBriefing.forConcept(decision.conceptId());
        if (decision.learnerAskedAbout() != null
                && !decision.learnerAskedAbout().equals(decision.conceptId())) {
            briefing = theoryBriefing.forConcept(decision.learnerAskedAbout()) + briefing;
        }
        if (!briefing.isEmpty()) {
            builder.append('\n').append(briefing);
        }
        return builder.toString();
    }

    public String exerciseBlock(String prompt, String answerMode) {
        return exerciseBlock(prompt, answerMode, null);
    }

    public String exerciseBlock(String prompt, String answerMode, String taskKind) {
        if (prompt == null) {
            return "No exercise this turn. Do not invent one that expects a specific answer.\n";
        }
        String framing = taskKind == null ? "" :
                "This turn asks the learner to %s.%n".formatted(taskKind);
        return """
                Work this question into your turn. Reword it so it reads as part of what you are \
                saying, but do not change what is being asked and do not answer it yourself:
                "%s"
                The learner will answer by: %s
                """.formatted(prompt, answerMode) + framing;
    }
}
