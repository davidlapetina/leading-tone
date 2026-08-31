package fr.lapetina.music.tutor;

import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.exercise.AnswerNormalizer;
import fr.lapetina.music.exercise.AttemptResult;
import fr.lapetina.music.exercise.Exercise;
import fr.lapetina.music.exercise.ExerciseService;
import fr.lapetina.music.exercise.Scaffold;
import fr.lapetina.music.learner.Learner;
import fr.lapetina.music.learner.LearnerService;
import fr.lapetina.music.learner.LearnerSnapshot;
import fr.lapetina.music.llm.TutorModel;
import fr.lapetina.music.llm.TutorRequest;
import fr.lapetina.music.midi.MidiPerformance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;

/**
 * The loop.
 *
 * <p>Read the learner model, decide what to do, generate the exercise, ask the language
 * model for the words, record what comes back. The order is the whole design: by the
 * time the model is called, every decision that matters has already been made.
 */
@ApplicationScoped
public class TutorOrchestrator {

    @Inject
    LearnerService learnerService;

    @Inject
    TeachingPolicy teachingPolicy;

    @Inject
    FocusDetector focusDetector;

    @Inject
    PracticeChoiceDetector practiceChoiceDetector;

    @Inject
    ExerciseService exerciseService;

    @Inject
    SessionService sessionService;

    @Inject
    TutorModel tutorModel;

    @Inject
    fr.lapetina.music.knowledge.router.KnowledgeRouter knowledgeRouter;

    @Inject
    fr.lapetina.music.knowledge.provenance.ProvenanceService provenanceService;

    @Inject
    fr.lapetina.music.concept.ConceptGraph conceptGraph;

    @Inject
    fr.lapetina.music.llm.tools.TurnScope turnScope;

    /** The first turn of a session: nothing has been said yet. */
    public TutorTurn open(Learner learner, TutorSession session) {
        return turn(learner, session, null, null, null, Optional.empty(), null);
    }

    /** The learner typed something that is not an answer to an exercise. */
    public TutorTurn message(Learner learner, TutorSession session, String message) {
        sessionService.append(session, InteractionRole.LEARNER, message, null, null, null);
        Learner updated = applyPracticeChoice(learner, message);
        return turn(updated, session, message, null, null, focusDetector.detect(message), null);
    }

    /**
     * "Let me play these instead" is neither an answer nor a question about theory. Without
     * this it would be marked wrong, which is a poor reward for telling the tutor how you
     * learn.
     */
    private Learner applyPracticeChoice(Learner learner, String message) {
        return practiceChoiceDetector.detect(message)
                .map(choice -> learnerService.choosePracticeMode(choice.orElse(null)))
                .orElse(learner);
    }

    /**
     * The learner answered an exercise in words — or asked for help with it, which is not
     * the same thing and must not be marked wrong.
     */
    public TutorTurn answerWithText(Learner learner, TutorSession session, UUID exerciseId, String answer) {
        Exercise exercise = requireExercise(exerciseId);
        sessionService.append(session, InteractionRole.LEARNER, answer, null, null, null);

        if (AnswerNormalizer.isRequestForHelp(answer)) {
            // Keep the same question open and say more about it. No evidence either way.
            return turn(learner, session, answer, null, null,
                    Optional.of(new LearnerFocus(exercise.conceptId, answer)), exercise);
        }

        // Nor is telling the tutor how you would rather practise.
        if (practiceChoiceDetector.detect(answer).isPresent()) {
            Learner updated = applyPracticeChoice(learner, answer);
            return turn(updated, session, answer, null, null, Optional.empty(), null);
        }

        // A question asked while an exercise happens to be open is still a question.
        // Grading it would record "does not know this concept" for someone who was asking
        // about a different one entirely.
        Optional<LearnerFocus> asked = focusDetector.detect(answer);
        if (asked.isPresent() && AnswerNormalizer.isQuestion(answer)) {
            return turn(learner, session, answer, null, null, asked, null);
        }

        AttemptResult result = exerciseService.answerWithText(exercise, answer);
        return turn(learner, session, answer, result, result.outcome().feedback(),
                Optional.empty(), null);
    }

    /** The learner answered an exercise by playing. */
    public TutorTurn answerWithMidi(Learner learner, TutorSession session, UUID exerciseId,
                                    MidiPerformance performance) {
        Exercise exercise = requireExercise(exerciseId);
        AttemptResult result = exerciseService.answerWithMidi(exercise, performance);
        String played = performance.spelled().stream()
                .map(fr.lapetina.music.theory.Note::name)
                .reduce((a, b) -> a + " " + b)
                .orElse("nothing");
        sessionService.append(session, InteractionRole.LEARNER, "(played " + played + ")", null, null, null);
        return turn(learner, session, "played " + played, result, result.outcome().feedback(),
                Optional.empty(), null);
    }

    /**
     * The verdict, with the question and the right answer alongside it.
     *
     * <p>Without the question, a model confirming a correct answer restates it from
     * memory and gets it wrong — telling a learner that B flat is a minor second above C
     * when the question said A. Everything here is computed, so the confirmation cannot
     * drift from what was actually asked.
     */
    private static String directiveFor(AttemptResult result) {
        if (result == null) {
            return null;
        }
        boolean right = result.outcome().result() == fr.lapetina.music.learner.EvidenceResult.CORRECT;
        return """
                They answered the previous question and it was %s.
                Acknowledge that in a few words — "that's right", "not quite" — and then move on to
                the question below. Do not restate the previous question, their answer, or any note,
                chord or key from it. You will get it wrong, and the interface has already shown them
                the exact verdict."""
                .formatted(right ? "correct" : "not correct");
    }

    /**
     * Everything funnels through here: policy first, exercise second, language last.
     *
     * @param reuseExercise the still-open exercise to keep on the table, when the learner
     *                      asked for help rather than answering
     */
    private TutorTurn turn(Learner learner, TutorSession session, String learnerMessage,
                           AttemptResult attempt, String evaluationFeedback,
                           Optional<LearnerFocus> focus, Exercise reuseExercise) {
        LearnerSnapshot snapshot = learnerService.snapshot(learner);
        TeachingDecision decision = teachingPolicy.next(snapshot, focus);

        Exercise exercise = reuseExercise;
        if (exercise == null && decision.expectsAnswer()) {
            // Someone who keeps missing this concept gets a smaller step, not the same
            // question again. The evidence it yields is weighted for the help given.
            Scaffold scaffold = Scaffold.forConsecutiveFailures(
                    snapshot.concept(decision.conceptId()).consecutiveFailures());
            exercise = exerciseService.create(learner, session.id, decision.conceptId(),
                    decision.difficulty(), decision.preferredAnswerMode(),
                    decision.action().preferredTaskKind(), scaffold);
        }

        // Everything the answer will rest on is gathered before the model is called: what
        // the engine computed, what the scores actually contain, and what the sources say.
        // The model's job is to teach from evidence, not to be the evidence.
        var concept = conceptGraph.find(decision.conceptId());
        var knowledge = knowledgeRouter.gather(
                decision.conceptId(),
                concept.map(fr.lapetina.music.concept.Concept::name).orElse(decision.conceptName()),
                concept.map(fr.lapetina.music.concept.Concept::description).orElse(null),
                learnerMessage != null ? learnerMessage : decision.learnerAskedAbout());

        TutorRequest request = new TutorRequest(session.id, snapshot, decision, exercise, learnerMessage,
                evaluationFeedback, directiveFor(attempt), knowledge);
        turnScope.beginTurn(decision.conceptId());
        String message = tutorModel.respond(request);

        String notation = exercise == null ? null : exercise.notationAbc;
        Interaction interaction = sessionService.append(session, InteractionRole.TUTOR, message,
                decision, exercise, notation);
        // Recorded after the interaction exists, so an answer can be traced to its evidence.
        provenanceService.record(session.id, interaction.id, decision.conceptId(), knowledge);

        return new TutorTurn(
                session.id,
                interaction.id,
                message,
                decision.action(),
                decision.conceptId(),
                decision.conceptName(),
                decision.rationale(),
                decision.difficulty(),
                exercise != null,
                exercise == null ? AnswerMode.NONE : exercise.answerMode,
                exercise == null ? null : exercise.id,
                exercise == null ? null : exercise.prompt,
                exercise == null ? null : exercise.taskKind.describe(),
                notation,
                attempt,
                tutorModel.describe());
    }

    private Exercise requireExercise(UUID exerciseId) {
        return exerciseService.find(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("No such exercise: " + exerciseId));
    }

    /** Exposed for the API: what the policy would do right now, without teaching anything. */
    public TeachingDecision preview(Learner learner) {
        return teachingPolicy.next(learnerService.snapshot(learner));
    }
}
