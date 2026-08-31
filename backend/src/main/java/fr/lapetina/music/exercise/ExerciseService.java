package fr.lapetina.music.exercise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.learner.Evidence;
import fr.lapetina.music.learner.EvidenceObservation;
import fr.lapetina.music.learner.EvidenceService;
import fr.lapetina.music.learner.EvidenceType;
import fr.lapetina.music.learner.Learner;
import fr.lapetina.music.learner.LearnerConcept;
import fr.lapetina.music.learner.MisconceptionService;
import fr.lapetina.music.midi.MidiPerformance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Persists generated exercises and turns answers into evidence.
 *
 * <p>The order matters: the answer is judged first, the judgement is stored, and only
 * then does the learner model move.
 */
@ApplicationScoped
public class ExerciseService {

    @Inject
    ExerciseGenerator generator;

    @Inject
    ExerciseEvaluator evaluator;

    @Inject
    EvidenceService evidenceService;

    @Inject
    MisconceptionService misconceptionService;

    @Inject
    ObjectMapper objectMapper;

    /** The caller wants this channel. Used by the API, where the request said so outright. */
    @Transactional
    public Exercise create(Learner learner, UUID sessionId, String conceptId, double difficulty,
                           AnswerMode requiredMode) {
        return create(learner, sessionId, generator.generate(conceptId, difficulty,
                chooseShape(learner, conceptId, requiredMode, null, true)));
    }

    /**
     * The policy suggests a channel; the tutor may rotate away from it for variety, unless
     * the learner has said which they want.
     */
    @Transactional
    public Exercise create(Learner learner, UUID sessionId, String conceptId, double difficulty,
                           AnswerMode suggestedMode, TaskKind preferredKind) {
        return create(learner, sessionId, conceptId, difficulty, suggestedMode, preferredKind, Scaffold.NONE);
    }

    @Transactional
    public Exercise create(Learner learner, UUID sessionId, String conceptId, double difficulty,
                           AnswerMode suggestedMode, TaskKind preferredKind, Scaffold scaffold) {
        return create(learner, sessionId, generator.generate(conceptId, difficulty,
                chooseShape(learner, conceptId, suggestedMode, preferredKind, false), scaffold));
    }

    /**
     * Picks which of a concept's forms to use next.
     *
     * <p>Three things decide it, in order. A learner who has said how they want to practise
     * gets that. The teaching action asks for a kind of task — challenging someone means
     * analysing, not spelling. Beyond that the tutor rotates, because asking the same form
     * twice running is how a lesson turns into a form letter.
     */
    ExerciseShape chooseShape(Learner learner, String conceptId, AnswerMode mode,
                              TaskKind preferredKind, boolean modeIsRequired) {
        List<ExerciseShape> menu = ExerciseGenerator.shapesFor(conceptId);

        AnswerMode required = modeIsRequired ? mode : learner.preferredAnswerMode;
        List<ExerciseShape> allowed = required == null ? menu
                : menu.stream().filter(shape -> shape.mode() == required).toList();
        if (allowed.isEmpty()) {
            allowed = menu;
        }
        if (allowed.size() == 1) {
            return allowed.get(0);
        }

        if (preferredKind != null) {
            List<ExerciseShape> ofKind = allowed.stream()
                    .filter(shape -> shape.kind() == preferredKind).toList();
            if (!ofKind.isEmpty()) {
                allowed = ofKind;
            }
        }

        ExerciseShape last = lastShapeFor(learner, conceptId);
        if (last != null && allowed.size() > 1) {
            List<ExerciseShape> fresh = allowed.stream().filter(shape -> !shape.equals(last)).toList();
            if (!fresh.isEmpty()) {
                allowed = fresh;
            }
        }
        // The remaining choice is arbitrary, so make it arbitrary rather than always the first.
        return allowed.get(ThreadLocalRandom.current().nextInt(allowed.size()));
    }

    private ExerciseShape lastShapeFor(Learner learner, String conceptId) {
        Exercise last = Exercise
                .<Exercise>find("learner = ?1 and conceptId = ?2 order by createdAt desc", learner, conceptId)
                .firstResult();
        return last == null ? null : ExerciseGenerator.shapesFor(conceptId).stream()
                .filter(shape -> shape.mode() == last.answerMode && shape.kind() == last.taskKind)
                .findFirst().orElse(null);
    }

    @Transactional
    public Exercise create(Learner learner, UUID sessionId, ExerciseSpec spec) {
        Exercise exercise = new Exercise();
        exercise.learner = learner;
        exercise.sessionId = sessionId;
        exercise.conceptId = spec.conceptId();
        exercise.exerciseType = spec.type();
        exercise.answerMode = spec.answerMode();
        exercise.prompt = spec.prompt();
        exercise.expectedAnswer = writeExpected(spec.expectedAnswer());
        exercise.keyContext = spec.keyContext();
        exercise.difficulty = spec.difficulty();
        exercise.notationAbc = spec.notationAbc();
        exercise.evidenceType = spec.evidenceType();
        exercise.taskKind = spec.taskKind();
        exercise.scaffold = spec.scaffold();
        exercise.choices = spec.choices().isEmpty() ? null : String.join("\n", spec.choices());
        exercise.persist();
        return exercise;
    }

    public Optional<Exercise> find(UUID id) {
        return Exercise.findByIdOptional(id);
    }

    public ExpectedAnswer expectedAnswerOf(Exercise exercise) {
        try {
            return objectMapper.readValue(exercise.expectedAnswer, ExpectedAnswer.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt expected answer on exercise " + exercise.id, e);
        }
    }

    @Transactional
    public AttemptResult answerWithText(Exercise exercise, String answer) {
        ExpectedAnswer expected = expectedAnswerOf(exercise);
        EvaluationOutcome outcome = evaluator.evaluateText(expected, answer, exercise.keyContext);
        EvidenceType type = expected.kind() == ExpectedAnswerKind.EXPLANATION
                ? EvidenceType.EXPLANATION
                : EvidenceType.TEXT_RECALL;
        return apply(exercise, answer, outcome, type);
    }

    @Transactional
    public AttemptResult answerWithMidi(Exercise exercise, MidiPerformance performance) {
        ExpectedAnswer expected = expectedAnswerOf(exercise);
        EvaluationOutcome outcome = evaluator.evaluateMidi(expected, performance, exercise.keyContext);
        return apply(exercise, performance.notes().toString(), outcome, midiEvidenceType(expected));
    }

    private static EvidenceType midiEvidenceType(ExpectedAnswer expected) {
        return switch (expected.kind()) {
            case MIDI_CHORD -> EvidenceType.MIDI_CHORD;
            case MIDI_SCALE -> EvidenceType.MIDI_SCALE;
            case MIDI_NOTES -> expected.noteNames().size() > 1 ? EvidenceType.MIDI_INTERVAL : EvidenceType.MIDI_NOTE;
            default -> EvidenceType.MIDI_NOTE;
        };
    }

    private AttemptResult apply(Exercise exercise, String rawAnswer, EvaluationOutcome outcome, EvidenceType type) {
        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.exerciseId = exercise.id;
        attempt.rawAnswer = rawAnswer == null ? "" : rawAnswer;
        attempt.correct = outcome.isCorrect();
        attempt.partial = outcome.result() == fr.lapetina.music.learner.EvidenceResult.PARTIALLY_CORRECT;
        attempt.feedback = outcome.feedback();
        attempt.detail = outcome.detail();
        attempt.persist();

        if (outcome.isCorrect()) {
            exercise.solved = true;
        }

        LearnerConcept before = LearnerConcept.findOrCreate(exercise.learner, exercise.conceptId);
        double masteryBefore = before.mastery;

        // A verdict the model still has to supply is not evidence yet.
        if (outcome.requiresModelJudgement()) {
            return new AttemptResult(outcome, exercise.conceptId, masteryBefore, masteryBefore, before.state, false);
        }

        EvidenceObservation observation = new EvidenceObservation(exercise.conceptId, type, outcome.result(),
                exercise.difficulty, outcome.confidence(), exercise.prompt,
                exercise.sessionId, null, exercise.id);
        Evidence evidence = evidenceService.record(exercise.learner, observation);

        if (outcome.misconceptionCode() != null) {
            misconceptionService.observe(exercise.learner, exercise.conceptId,
                    outcome.misconceptionCode(), outcome.misconceptionDescription());
        } else if (outcome.isCorrect()) {
            misconceptionService.openFor(exercise.learner, exercise.conceptId)
                    .forEach(misconception -> misconceptionService.resolve(exercise.learner,
                            misconception.conceptId, misconception.code));
        }

        LearnerConcept after = LearnerConcept.findOrCreate(exercise.learner, exercise.conceptId);
        return new AttemptResult(outcome, exercise.conceptId, evidence.masteryBefore, after.mastery,
                after.state, true);
    }

    private String writeExpected(ExpectedAnswer expected) {
        try {
            return objectMapper.writeValueAsString(expected);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise the expected answer", e);
        }
    }
}
