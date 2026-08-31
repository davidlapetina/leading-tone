package fr.lapetina.music.tutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.exercise.Exercise;
import fr.lapetina.music.exercise.ExerciseGenerator;
import fr.lapetina.music.exercise.ExerciseShape;
import fr.lapetina.music.exercise.TaskKind;
import fr.lapetina.music.exercise.ExerciseService;
import fr.lapetina.music.learner.Learner;
import fr.lapetina.music.learner.LearnerService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The tutor asking "play F3", then "play G3", then "play A3" is one question wearing
 * several hats. These cover the two things that stop it: alternating between a concept's
 * forms, and honouring a learner who has said which form they want.
 */
@QuarkusTest
class ExerciseVarietyTest {

    @Inject
    ExerciseService exerciseService;

    @Inject
    LearnerService learnerService;

    @BeforeEach
    void startFromNothing() {
        learnerService.reset();
    }

    /** The shapes the tutor chose over a run, as (kind, channel) pairs. */
    private List<ExerciseShape> shapesOver(int turns, String conceptId) {
        Learner learner = learnerService.current();
        List<ExerciseShape> shapes = new ArrayList<>();
        for (int i = 0; i < turns; i++) {
            Exercise exercise = exerciseService.create(learner, null, conceptId, 0.5,
                    AnswerMode.MIDI, null);
            shapes.add(new ExerciseShape(exercise.taskKind, exercise.answerMode));
        }
        return shapes;
    }

    @Test
    @DisplayName("the same form is never asked twice running")
    void neverAsksTheSameFormTwiceRunning() {
        List<ExerciseShape> shapes = shapesOver(8, "triad");
        for (int i = 1; i < shapes.size(); i++) {
            assertTrue(!shapes.get(i).equals(shapes.get(i - 1)),
                    "asked the same form twice running: " + shapes);
        }
    }

    @Test
    @DisplayName("a run covers several kinds of thinking, not one repeated")
    void drawsOnTheWholeMenu() {
        Set<TaskKind> kinds = new java.util.HashSet<>();
        shapesOver(12, "roman-numeral").forEach(shape -> kinds.add(shape.kind()));
        assertTrue(kinds.size() >= 3, "only exercised " + kinds);
    }

    @Test
    @DisplayName("a learner who chose how to practise is not varied away from it")
    void anExplicitChoiceIsLeftAlone() {
        learnerService.choosePracticeMode(AnswerMode.MIDI);
        for (ExerciseShape shape : shapesOver(5, "triad")) {
            assertEquals(AnswerMode.MIDI, shape.mode());
        }
    }

    @Test
    @DisplayName("a concept with nothing to play is never asked for at the keyboard")
    void neverOffersAFormThatDoesNotExist() {
        Learner learner = learnerService.current();
        learnerService.choosePracticeMode(AnswerMode.MIDI);
        Exercise exercise = exerciseService.create(learner, null, "key-signature", 0.5, AnswerMode.MIDI);
        assertEquals(AnswerMode.TEXT, exercise.answerMode);

        assertTrue(ExerciseGenerator.shapesFor("cadence").stream().noneMatch(ExerciseShape::isPlayed));
        assertTrue(ExerciseGenerator.shapesFor("triad").stream().anyMatch(ExerciseShape::isPlayed));
    }

    @Test
    @DisplayName("the teaching action decides what kind of thinking is asked for")
    void theActionChoosesTheKind() {
        Learner learner = learnerService.current();
        Exercise challenge = exerciseService.create(learner, null, "roman-numeral", 0.8,
                AnswerMode.TEXT, TaskKind.ANALYSE);
        assertEquals(TaskKind.ANALYSE, challenge.taskKind);

        Exercise practice = exerciseService.create(learner, null, "roman-numeral", 0.4,
                AnswerMode.TEXT, TaskKind.BUILD);
        assertEquals(TaskKind.BUILD, practice.taskKind);
    }
}
