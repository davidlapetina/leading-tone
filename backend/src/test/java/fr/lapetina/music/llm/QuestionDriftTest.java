package fr.lapetina.music.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.exercise.Exercise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Told to ask for A3, models write "now play F#3" while the exercise on screen still says
 * A3. Whichever the learner plays, one of the two is wrong and they cannot tell which.
 *
 * <p>The check is deliberately narrow: an explanation that mentions C, or the key of G, is
 * how teaching is done and must survive.
 */
class QuestionDriftTest {

    private static TutorRequest turnAsking(String prompt) {
        Exercise exercise = new Exercise();
        exercise.prompt = prompt;
        return new TutorRequest(null, null, null, exercise, null, null, null);
    }

    @Test
    @DisplayName("naming a different pitch than the exercise is caught")
    void catchesADifferentNote() {
        TutorRequest turn = turnAsking("Play A3 on the keyboard.");

        assertTrue(RoutingTutorModel.asksADifferentQuestion(
                "Good. Now, can you play F#3?", turn));
        assertTrue(RoutingTutorModel.asksADifferentQuestion(
                "You played A3. Let us try G4 next.", turn));
    }

    @Test
    @DisplayName("repeating the note it was told to ask for is fine")
    void allowsTheNoteThatWasAsked() {
        TutorRequest turn = turnAsking("Play A3 on the keyboard.");

        assertFalse(RoutingTutorModel.asksADifferentQuestion(
                "Find A3 — it is just below middle C.", turn));
        assertFalse(RoutingTutorModel.asksADifferentQuestion(
                "Listen for where it sits, then play it.", turn));
    }

    @Test
    @DisplayName("teaching with note names is untouched, because that is how teaching works")
    void doesNotFireOnOrdinaryExplanation() {
        TutorRequest turn = turnAsking("Which note is a minor second above C?");

        assertFalse(RoutingTutorModel.asksADifferentQuestion(
                "A minor second is one semitone. From C that is D flat, or C sharp.", turn),
                "the exercise names no octave, so there is no specific pitch to contradict");
        assertFalse(RoutingTutorModel.asksADifferentQuestion(
                "In the key of G, the leading tone is F sharp.", turn));
    }

    @Test
    void ignoresTurnsWithNoExercise() {
        assertFalse(RoutingTutorModel.asksADifferentQuestion(
                "Play F#3.", new TutorRequest(null, null, null, null, null, null, null)));
    }
}
