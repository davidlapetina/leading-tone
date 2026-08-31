package fr.lapetina.music.tutor;

import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.exercise.ExerciseGenerator;
import fr.lapetina.music.learner.ConceptMastery;
import fr.lapetina.music.learner.LearnerSnapshot;
import fr.lapetina.music.learner.LearningState;
import fr.lapetina.music.learner.MisconceptionView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Decides what to teach next, from the learner model alone.
 *
 * <p>This class is the reason the application is a tutor rather than a chatbot with a
 * syllabus. It runs before the language model is called, it is deterministic, and it is
 * unit-tested without any model in the loop.
 */
@ApplicationScoped
public class TeachingPolicy {

    static final double EXPLAIN_CEILING = 0.25;
    static final double PRACTICE_CEILING = 0.60;
    static final double CHALLENGE_CEILING = 0.85;

    /**
     * A prerequisite may be past the "known" line and still be too shaky to build on.
     * Above 0.45 a concept counts as known; below this it is not solid enough to carry
     * something new.
     */
    static final double PREREQUISITE_FLOOR = 0.60;

    /** A misconception is worth interrupting for once it has been seen this often. */
    static final int MISCONCEPTION_URGENCY = 2;

    @Inject
    ConceptGraph conceptGraph;

    public TeachingDecision next(LearnerSnapshot snapshot) {
        return next(snapshot, Optional.empty());
    }

    /**
     * The order of precedence. A misconception that keeps firing is corrected first
     * because everything built on it will be wrong; after that, what the learner actually
     * asked about beats what the tutor had planned, because ignoring a direct question is
     * how a tutor stops being one.
     */
    public TeachingDecision next(LearnerSnapshot snapshot, Optional<LearnerFocus> focus) {
        if (snapshot.isBlank() && focus.isEmpty() && !snapshot.isFreeMode()) {
            return diagnose(snapshot);
        }
        Optional<TeachingDecision> misconception = correctMisconception(snapshot);
        if (misconception.isPresent()) {
            return misconception.get();
        }
        Optional<TeachingDecision> asked = answerQuestion(snapshot, focus);
        if (asked.isPresent()) {
            return asked.get();
        }
        // Free mode: the learner picked the subject, so reviews of other things wait.
        if (snapshot.focusConceptId() != null) {
            ConceptMastery target = snapshot.concept(snapshot.focusConceptId());
            Optional<TeachingDecision> shaky = target.state() == LearningState.UNKNOWN
                    ? reinforceWeakPrerequisite(snapshot, target)
                    : Optional.empty();
            return shaky.orElseGet(() -> decisionFor(snapshot, target, actionFor(target)));
        }
        Optional<TeachingDecision> review = review(snapshot);
        if (review.isPresent()) {
            return review.get();
        }
        return advance(snapshot);
    }

    /**
     * Takes up what the learner raised. If the groundwork for it is missing, the tutor
     * goes to the nearest missing prerequisite instead — but the decision still records
     * what was asked, so the answer can begin by acknowledging it rather than ignoring it.
     */
    private Optional<TeachingDecision> answerQuestion(LearnerSnapshot snapshot, Optional<LearnerFocus> focus) {
        if (focus.isEmpty()) {
            return Optional.empty();
        }
        String askedAbout = focus.get().conceptId();
        ConceptMastery target = snapshot.concept(askedAbout);

        List<Concept> missing = conceptGraph.missingPrerequisites(askedAbout, snapshot::knows);
        if (!missing.isEmpty()) {
            ConceptMastery groundwork = snapshot.concept(missing.get(missing.size() - 1).id());
            return Optional.of(new TeachingDecision(TeachingAction.REINFORCE,
                    groundwork.conceptId(), groundwork.name(),
                    supportingConcepts(snapshot, groundwork.conceptId()),
                    difficultyFor(groundwork),
                    answerModeFor(groundwork, snapshot),
                    "Asked about %s, but %s is not in place yet".formatted(askedAbout, groundwork.conceptId()),
                    null).answering(askedAbout));
        }
        return Optional.of(new TeachingDecision(TeachingAction.ANSWER_QUESTION,
                target.conceptId(), target.name(),
                supportingConcepts(snapshot, target.conceptId()),
                difficultyFor(target),
                answerModeFor(target, snapshot),
                "The learner asked about %s".formatted(askedAbout),
                null).answering(askedAbout));
    }

    /** With nothing known, start from the most foundational concept and just talk. */
    private TeachingDecision diagnose(LearnerSnapshot snapshot) {
        Concept first = conceptGraph.all().stream()
                .filter(concept -> concept.prerequisites().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The concept graph has no root"));
        return new TeachingDecision(TeachingAction.DIAGNOSE, first.id(), first.name(), List.of(),
                0.35, AnswerMode.TEXT,
                "Nothing is known about this learner yet, so start by finding out rather than teaching.", null);
    }

    private Optional<TeachingDecision> correctMisconception(LearnerSnapshot snapshot) {
        return snapshot.openMisconceptions().stream()
                .filter(misconception -> misconception.occurrences() >= MISCONCEPTION_URGENCY)
                .max(Comparator.comparingInt(MisconceptionView::occurrences))
                .map(misconception -> {
                    ConceptMastery concept = snapshot.concept(misconception.conceptId());
                    return new TeachingDecision(TeachingAction.CORRECT_MISCONCEPTION,
                            concept.conceptId(), concept.name(),
                            supportingConcepts(snapshot, concept.conceptId()),
                            difficultyFor(concept),
                            answerModeFor(concept, snapshot),
                            "Seen %d times: %s".formatted(misconception.occurrences(), misconception.description()),
                            misconception);
                });
    }

    private Optional<TeachingDecision> review(LearnerSnapshot snapshot) {
        return snapshot.dueForReview().stream()
                .min(Comparator.comparingDouble(ConceptMastery::mastery))
                .map(concept -> new TeachingDecision(TeachingAction.REVIEW, concept.conceptId(), concept.name(),
                        supportingConcepts(snapshot, concept.conceptId()),
                        difficultyFor(concept),
                        answerModeFor(concept, snapshot),
                        "Due for review since %s, mastery %.2f".formatted(concept.nextReviewAt(), concept.mastery()),
                        null));
    }

    /**
     * Picks what to move forward on.
     *
     * <p>The frontier is everything whose prerequisites are in place but which is not yet
     * held. Something already started is finished before something new is begun, and
     * nothing new is begun on a prerequisite that is merely known rather than solid.
     */
    private TeachingDecision advance(LearnerSnapshot snapshot) {
        Comparator<ConceptMastery> order = Comparator
                .comparing((ConceptMastery concept) -> concept.state() == LearningState.UNKNOWN)
                .thenComparingDouble(concept -> conceptGraph.require(concept.conceptId()).intrinsicDifficulty());

        // A chosen area narrows the frontier without abandoning the prerequisite rules:
        // asking for "harmony" while triads are shaky still sends the tutor back a step.
        List<Concept> frontier = conceptGraph.frontier(snapshot::knows).stream()
                .filter(concept -> withinFocus(snapshot, concept))
                .toList();
        if (frontier.isEmpty()) {
            frontier = conceptGraph.frontier(snapshot::knows);
        }

        Optional<ConceptMastery> candidate = frontier.stream()
                .map(concept -> snapshot.concept(concept.id()))
                .min(order);

        if (candidate.isPresent()) {
            ConceptMastery target = candidate.get();
            if (target.state() == LearningState.UNKNOWN) {
                Optional<TeachingDecision> shaky = reinforceWeakPrerequisite(snapshot, target);
                if (shaky.isPresent()) {
                    return shaky.get();
                }
            }
            return decisionFor(snapshot, target, actionFor(target));
        }

        // Everything within reach is held. Consolidate the weakest of it, or push it further.
        ConceptMastery weakest = snapshot.concepts().stream()
                .filter(concept -> concept.state() != LearningState.UNKNOWN)
                .min(Comparator.comparingDouble(ConceptMastery::mastery))
                .orElseThrow(() -> new IllegalStateException("The snapshot has no observed concepts"));
        return decisionFor(snapshot, weakest, actionFor(weakest));
    }

    private boolean withinFocus(LearnerSnapshot snapshot, Concept concept) {
        return snapshot.focusCategory() == null
                || concept.category().name().equalsIgnoreCase(snapshot.focusCategory());
    }

    private TeachingDecision decisionFor(LearnerSnapshot snapshot, ConceptMastery target, TeachingAction action) {
        String rationale = "%s is at mastery %.2f (%s); %s".formatted(
                target.conceptId(), target.mastery(), target.state(), reasonFor(action));
        return new TeachingDecision(action, target.conceptId(), target.name(),
                supportingConcepts(snapshot, target.conceptId()),
                difficultyFor(target),
                answerModeFor(target, snapshot),
                rationale, null);
    }

    /** Goes back one step when a direct prerequisite is not solid enough to build on. */
    private Optional<TeachingDecision> reinforceWeakPrerequisite(LearnerSnapshot snapshot, ConceptMastery target) {
        return conceptGraph.prerequisitesOf(target.conceptId()).stream()
                .map(prerequisite -> snapshot.concept(prerequisite.id()))
                .filter(prerequisite -> prerequisite.mastery() < PREREQUISITE_FLOOR)
                .min(Comparator.comparingDouble(ConceptMastery::mastery))
                .map(prerequisite -> new TeachingDecision(TeachingAction.REINFORCE,
                        prerequisite.conceptId(), prerequisite.name(),
                        supportingConcepts(snapshot, prerequisite.conceptId()),
                        difficultyFor(prerequisite),
                        answerModeFor(prerequisite, snapshot),
                        "%s is the next step but %s is only at mastery %.2f".formatted(
                                target.conceptId(), prerequisite.conceptId(), prerequisite.mastery()),
                        null));
    }

    TeachingAction actionFor(ConceptMastery concept) {
        if (concept.state() == LearningState.UNKNOWN) {
            return TeachingAction.INTRODUCE;
        }
        if (concept.mastery() < EXPLAIN_CEILING) {
            return TeachingAction.EXPLAIN;
        }
        if (concept.mastery() < PRACTICE_CEILING) {
            return TeachingAction.PRACTICE;
        }
        if (concept.mastery() < CHALLENGE_CEILING) {
            return TeachingAction.CHALLENGE;
        }
        return TeachingAction.TRANSFER;
    }

    private static String reasonFor(TeachingAction action) {
        return switch (action) {
            case INTRODUCE -> "it has not been met yet";
            case EXPLAIN -> "it has been met but has not settled";
            case PRACTICE -> "it needs repetition at its current level";
            case CHALLENGE -> "it is solid enough to push";
            case TRANSFER -> "it is reliable, so the test is whether it moves to new ground";
            case REINFORCE -> "it is carrying the next step and is not solid yet";
            default -> "the policy selected it";
        };
    }

    /** Mastered neighbours worth anchoring the explanation to. */
    private List<String> supportingConcepts(LearnerSnapshot snapshot, String conceptId) {
        List<String> supporting = new ArrayList<>();
        for (Concept prerequisite : conceptGraph.prerequisitesOf(conceptId)) {
            if (snapshot.knows(prerequisite.id())) {
                supporting.add(prerequisite.id());
            }
        }
        return List.copyOf(supporting);
    }

    /**
     * Aim slightly above current mastery, weighted by how hard the concept is in itself.
     * Too easy produces no evidence; too hard produces only frustration.
     */
    double difficultyFor(ConceptMastery concept) {
        Concept definition = conceptGraph.require(concept.conceptId());
        double stretch = concept.mastery() + 0.15;
        double blended = 0.6 * stretch + 0.4 * definition.intrinsicDifficulty();
        return Math.max(0.15, Math.min(0.95, Math.round(blended * 100.0) / 100.0));
    }

    /**
     * How to practise: what the learner asked for if they asked, otherwise the keyboard for
     * anything playable that they have been answering well that way.
     *
     * <p>Whether a concept can be played is asked of the generator rather than guessed from
     * the concept's category — key signatures are filed under scales and there is nothing
     * to play.
     */
    AnswerMode answerModeFor(ConceptMastery concept, LearnerSnapshot snapshot) {
        if (!ExerciseGenerator.supports(concept.conceptId(), AnswerMode.MIDI)) {
            return AnswerMode.TEXT;
        }
        if (snapshot.preferredAnswerMode() != null) {
            return snapshot.preferredAnswerMode();
        }
        double keyboard = snapshot.preferences().getOrDefault("keyboardPreference", 0.5);
        return keyboard >= 0.5 ? AnswerMode.MIDI : AnswerMode.TEXT;
    }
}
