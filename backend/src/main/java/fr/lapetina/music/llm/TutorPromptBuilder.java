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

    /** Enough to steer the explanation, not so many that the prompt becomes a syllabus. */
    private static final int MAX_LISTED_PREREQUISITES = 6;

    @org.eclipse.microprofile.config.inject.ConfigProperty(
            name = "music.knowledge.prompt.max-chars", defaultValue = "2600")
    int promptMaxChars;

    @org.eclipse.microprofile.config.inject.ConfigProperty(
            name = "music.knowledge.prompt.max-chars-per-chunk", defaultValue = "900")
    int promptMaxCharsPerChunk;

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
        return instruction(decision, (LearnerSnapshot) null);
    }

    /** The instruction, told what the learner already has so it does not re-teach it. */
    public String instruction(TeachingDecision decision, LearnerSnapshot snapshot) {
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
        builder.append(prerequisiteGuidance(decision, snapshot));
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

    /**
     * What not to re-teach.
     *
     * <p>The learner model exists to be used, and the most visible way to use it is to stop
     * explaining things somebody already knows. Asked "what is V/V?" by someone solid on
     * dominant function, the answer should begin from the dominant, not from what a scale
     * is. Listing the prerequisites they have is what lets the model do that; listing the
     * ones they lack is what stops it assuming too much.
     */
    String prerequisiteGuidance(TeachingDecision decision, LearnerSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        List<Concept> prerequisites = conceptGraph.allPrerequisitesOf(decision.conceptId());
        if (prerequisites.isEmpty()) {
            return "";
        }
        List<String> solid = prerequisites.stream()
                .filter(concept -> snapshot.knows(concept.id()))
                .map(Concept::name)
                .limit(MAX_LISTED_PREREQUISITES)
                .toList();
        List<String> shaky = prerequisites.stream()
                .filter(concept -> !snapshot.knows(concept.id()))
                .map(Concept::name)
                .limit(MAX_LISTED_PREREQUISITES)
                .toList();

        StringBuilder guidance = new StringBuilder();
        if (!solid.isEmpty()) {
            guidance.append("Already solid, so build on these rather than explaining them: ")
                    .append(String.join(", ", solid)).append(".\n");
        }
        if (!shaky.isEmpty()) {
            guidance.append("Not yet solid, so do not assume them: ")
                    .append(String.join(", ", shaky)).append(".\n");
        }
        return guidance.toString();
    }

    /**
     * The instruction, plus whatever was gathered for this turn.
     *
     * <p>Computed facts, then verified examples, then quoted prose — in that order, and told
     * apart, so the model knows which of them it is allowed to contradict.
     */
    public String instruction(TeachingDecision decision,
                              fr.lapetina.music.knowledge.router.TutorKnowledge knowledge,
                              LearnerSnapshot snapshot) {
        return instruction(decision, snapshot)
                + fr.lapetina.music.knowledge.router.KnowledgeBlock.render(
                        knowledge, promptMaxChars, promptMaxCharsPerChunk);
    }

    public String exerciseBlock(String prompt, String answerMode) {
        return exerciseBlock(prompt, answerMode, null);
    }

    public String exerciseBlock(String prompt, String answerMode, String taskKind) {
        return exerciseBlock(prompt, answerMode, taskKind, false);
    }

    /**
     * @param opening whether this is the first thing the learner sees this session, which the
     *     model cannot tell from an empty conversation and will otherwise guess wrongly --
     *     it opens with "you just wrote", referring to a turn that never happened
     */
    public String exerciseBlock(String prompt, String answerMode, String taskKind, boolean opening) {
        String start = opening
                ? "This is the first thing the learner sees this session. Nothing has been said "
                        + "yet, so do not refer to anything they have just done or just answered.\n"
                : "";
        if (prompt == null) {
            return start + "No exercise this turn. Do not invent one that expects a specific answer.\n";
        }
        String framing = taskKind == null ? "" :
                "This turn asks the learner to %s.%n".formatted(taskKind);
        return start + """
                Work this question into your turn. Reword it so it reads as part of what you are \
                saying, but do not change what is being asked and do not answer it yourself:
                "%s"
                The learner will answer by: %s
                """.formatted(prompt, answerMode) + framing;
    }
}
