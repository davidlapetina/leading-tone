package fr.lapetina.music.exercise;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnswerNormalizerTest {

    @Test
    @DisplayName("asking for help is not an attempt at the answer")
    void recognisesRequestsForHelp() {
        for (String text : new String[]{"explain", "Explain.", "explain that", "why?", "help",
                "what do you mean", "I don't understand", "hint", "tell me more"}) {
            assertTrue(AnswerNormalizer.isRequestForHelp(text), text);
        }
    }

    @Test
    void doesNotMistakeAnAnswerForARequestForHelp() {
        for (String text : new String[]{"G7", "F Ab C", "a major third", "V7/V", "3 sharps",
                "because the leading tone pulls up"}) {
            assertFalse(AnswerNormalizer.isRequestForHelp(text), text);
            assertFalse(AnswerNormalizer.isDontKnow(text), text);
        }
    }

    @Test
    @DisplayName("a question is a question, even when an exercise is open")
    void recognisesQuestions() {
        for (String text : new String[]{"what is a C major add 7 chord", "why does it resolve",
                "how do inversions work", "can you explain cadences", "tell me about modes",
                "What's a secondary dominant?"}) {
            assertTrue(AnswerNormalizer.isQuestion(text), text);
        }
    }

    @Test
    @DisplayName("a hesitant answer is still an answer and must be graded")
    void doesNotMistakeAHesitantAnswerForAQuestion() {
        for (String text : new String[]{"predominant?", "G7", "a major third", "V7/V?", "F Ab C",
                "3 sharps", "first inversion"}) {
            assertFalse(AnswerNormalizer.isQuestion(text), text);
        }
    }

    @Test
    @DisplayName("saying you do not know is a skip, not a mistake")
    void recognisesNotKnowing() {
        for (String text : new String[]{"I don't know", "no idea", "not sure", "dunno", "skip", "pass"}) {
            assertTrue(AnswerNormalizer.isDontKnow(text), text);
        }
    }
}
