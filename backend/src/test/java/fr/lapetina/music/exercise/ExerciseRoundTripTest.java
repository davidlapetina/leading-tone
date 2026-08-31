package fr.lapetina.music.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import fr.lapetina.music.learner.EvidenceResult;
import fr.lapetina.music.midi.MidiEvaluator;
import fr.lapetina.music.midi.MidiPerformance;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Note;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Generation and evaluation are two halves of the same claim, so they are tested against
 * each other: every exercise the generator can produce, at every difficulty, must accept
 * the answer the generator itself says is correct.
 *
 * <p>This is what stops a question drifting away from its own answer key.
 */
class ExerciseRoundTripTest {

    private static final List<Double> DIFFICULTIES = List.of(0.2, 0.5, 0.85);
    private static final int ROUNDS = 12;

    private ConceptGraph graph;
    private ExerciseEvaluator evaluator;

    @BeforeEach
    void setUp() {
        graph = new ConceptGraph(new ObjectMapper());
        evaluator = new ExerciseEvaluator();
        evaluator.midiEvaluator = new MidiEvaluator();
    }

    @Test
    @DisplayName("every generated written exercise accepts its own canonical answer")
    void writtenExercisesAcceptTheirOwnAnswer() {
        ExerciseGenerator generator = new ExerciseGenerator(new Random(20260831L));
        List<String> failures = new ArrayList<>();

        for (Concept concept : graph.all()) {
            for (double difficulty : DIFFICULTIES) {
                for (ExerciseShape shape : ExerciseGenerator.shapesFor(concept.id())) {
                    if (shape.isPlayed()) {
                        continue;
                    }
                    for (int round = 0; round < ROUNDS; round++) {
                        ExerciseSpec spec = generator.generate(concept.id(), difficulty, shape);
                        ExpectedAnswer expected = spec.expectedAnswer();
                        if (expected.kind() == ExpectedAnswerKind.EXPLANATION) {
                            continue;
                        }
                        EvaluationOutcome outcome =
                                evaluator.evaluateText(expected, expected.canonical(), spec.keyContext());
                        if (outcome.result() != EvidenceResult.CORRECT) {
                            failures.add("%s %s @ %.2f: \"%s\" answered \"%s\" -> %s (%s)".formatted(
                                    concept.id(), shape, difficulty, spec.prompt(), expected.canonical(),
                                    outcome.result(), outcome.feedback()));
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), "Exercises rejected their own answers:\n" + String.join("\n", failures));
    }

    @Test
    @DisplayName("every generated keyboard exercise accepts the notes it asked for")
    void playedExercisesAcceptTheirOwnAnswer() {
        ExerciseGenerator generator = new ExerciseGenerator(new Random(451L));
        List<String> failures = new ArrayList<>();

        for (Concept concept : graph.all()) {
            for (double difficulty : DIFFICULTIES) {
                for (ExerciseShape shape : ExerciseGenerator.shapesFor(concept.id())) {
                    if (!shape.isPlayed()) {
                        continue;
                    }
                    for (int round = 0; round < ROUNDS; round++) {
                        ExerciseSpec spec = generator.generate(concept.id(), difficulty, shape);
                        MidiPerformance performance = MidiPerformance.of(correctNotes(spec.expectedAnswer()));
                        EvaluationOutcome outcome =
                                evaluator.evaluateMidi(spec.expectedAnswer(), performance, spec.keyContext());
                        if (outcome.result() != EvidenceResult.CORRECT) {
                            failures.add("%s %s @ %.2f: \"%s\" played %s -> %s (%s)".formatted(
                                    concept.id(), shape, difficulty, spec.prompt(), performance.notes(),
                                    outcome.result(), outcome.feedback()));
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), "Keyboard exercises rejected their own answers:\n"
                + String.join("\n", failures));
    }

    @Test
    @DisplayName("every concept offers more than one way to practise it")
    void everyConceptHasAMenu() {
        List<String> thin = new ArrayList<>();
        for (Concept concept : graph.all()) {
            List<ExerciseShape> shapes = ExerciseGenerator.shapesFor(concept.id());
            if (shapes.size() < 2) {
                thin.add(concept.id() + " has only " + shapes);
            }
            assertEquals(shapes.size(), Set.copyOf(shapes).size(), concept.id() + " lists a shape twice");
        }
        assertTrue(thin.isEmpty(), "concepts with nothing to choose between:\n" + String.join("\n", thin));
    }

    @Test
    void everyConceptCanProduceAnExercise() {
        ExerciseGenerator generator = new ExerciseGenerator(new Random(7L));
        for (Concept concept : graph.all()) {
            ExerciseSpec spec = generator.generate(concept.id(), 0.5, AnswerMode.TEXT);
            assertNotNull(spec.prompt(), concept.id());
            assertEquals(concept.id(), spec.conceptId());
            assertTrue(spec.prompt().endsWith(".") || spec.prompt().endsWith("?"),
                    concept.id() + " should read as a sentence: " + spec.prompt());
            assertNotNull(spec.expectedAnswer(), concept.id());
        }
    }

    /** Turns an expected answer back into the MIDI a learner would send if they got it right. */
    private static List<Integer> correctNotes(ExpectedAnswer expected) {
        return switch (expected.kind()) {
            case MIDI_CHORD -> ChordAnalyzer.parse(expected.chordSymbol()).notes(3).stream()
                    .map(Note::midi).toList();
            case MIDI_SCALE -> new Scale(PitchClass.parse(expected.scaleTonic()),
                    ScaleType.valueOf(expected.scaleType())).notes(4).stream()
                    .map(Note::midi).toList();
            case MIDI_NOTES, NOTE_SET, NOTE_SEQUENCE -> expected.noteNames().stream()
                    .map(name -> new Note(PitchClass.parse(name), 4).midi()).toList();
            default -> List.of();
        };
    }
}
