package fr.lapetina.music.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The model cannot see whether a session has started, and guesses badly when it has to.
 *
 * <p>Left to itself it opens with "You just wrote another name for Ab, so let's check what
 * you came up with" on the first screen a learner ever sees — referring to a turn that never
 * happened, directly under a line saying nothing is known about them yet.
 */
class OpeningTurnTest {

    private final TutorPromptBuilder builder = new TutorPromptBuilder();

    @Test
    @DisplayName("the first turn of a session says so, so the model does not invent a history")
    void tellsTheModelWhenNothingHasHappenedYet() {
        String opening = builder.exerciseBlock("Write another name for Ab.", "WRITTEN", null, true);

        assertTrue(opening.contains("first thing the learner sees"),
                "the model has to be told, because it cannot tell: " + opening);
        assertTrue(opening.contains("do not refer to anything they have just done"), opening);
        assertTrue(opening.contains("Write another name for Ab."),
                "the exercise still has to survive the framing: " + opening);
    }

    @Test
    @DisplayName("a turn in progress is not framed as an opening")
    void staysQuietOnceTheConversationHasStarted() {
        String later = builder.exerciseBlock("Write another name for Ab.", "WRITTEN", null, false);

        assertFalse(later.contains("first thing the learner sees"),
                "mid-conversation the model should refer back freely: " + later);
    }

    @Test
    @DisplayName("a turn with no exercise still says when it is the first")
    void framesAnOpeningWithNothingToAsk() {
        String opening = builder.exerciseBlock(null, null, null, true);

        assertTrue(opening.contains("first thing the learner sees"), opening);
        assertTrue(opening.contains("Do not invent one"), opening);
    }
}
