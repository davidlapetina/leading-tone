package fr.lapetina.music.knowledge.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which questions are answered by arithmetic, which by quotation, and which by looking in a
 * score. Getting this wrong is how a tutor ends up guessing at a calculation or inventing an
 * example, so the routing is deterministic and pinned here.
 */
class RoutingTest {

    private final IntentClassifier classifier = new IntentClassifier();

    private Set<RetrievalIntent> intents(String question) {
        return classifier.classify(question);
    }

    private String computed(String question) {
        return TheoryQuestion.answer(question).map(TheoryAnswer::answer).orElse(null);
    }

    @Test
    @DisplayName("anything with an arithmetic answer is computed, not asked of a model")
    void routesCalculationsToTheEngine() {
        assertEquals("D F# A C", computed("What is V7/V in C major?"));
        assertEquals("G B D F", computed("What notes are in G7?"));
        assertEquals("Ab", computed("What is a minor sixth above C?"));
        assertEquals("D E F# G A B C#", computed("Give me the D major scale."));
        assertEquals("Ab C Eb F#", computed("What is Ger+6 in C major?"));

        for (String question : new String[]{"What is V7/V in C major?", "What notes are in G7?",
                "Give me the D major scale.", "Transpose ii-V-I from C major to Eb major."}) {
            assertTrue(intents(question).contains(RetrievalIntent.DETERMINISTIC_CALCULATION), question);
        }
    }

    @Test
    void transposesAProgressionBetweenKeys() {
        assertEquals("Fm Bb Eb", computed("Transpose ii-V-I from C major to Eb major."));
    }

    @Test
    @DisplayName("a conceptual question is not sent to the calculator")
    void routesExplanationsToRetrieval() {
        for (String question : new String[]{
                "What is a secondary dominant?",
                "Why does the leading tone resolve upward?",
                "Explain modal mixture.",
                "What is tritone substitution?"}) {
            assertTrue(intents(question).contains(RetrievalIntent.CONCEPT_EXPLANATION), question);
            assertFalse(intents(question).contains(RetrievalIntent.HARMONIC_EXAMPLE), question);
        }
        assertEquals(null, computed("Why does the leading tone resolve upward?"),
                "nothing here is calculable, and pretending otherwise would invent an answer");
    }

    @Test
    @DisplayName("asking for real music is a request for a corpus lookup")
    void routesExamplesToTheCorpus() {
        for (String question : new String[]{
                "Give me a Beethoven example of V/V.",
                "Show me a Mozart deceptive cadence.",
                "Find a Chopin example of tonicization."}) {
            assertTrue(intents(question).contains(RetrievalIntent.HARMONIC_EXAMPLE), question);
        }
    }

    @Test
    @DisplayName("one question can want several things at once")
    void handlesSeveralIntentsInOneQuestion() {
        Set<RetrievalIntent> both = intents("Explain V7/V in C major and give me a Mozart example.");

        assertTrue(both.contains(RetrievalIntent.DETERMINISTIC_CALCULATION));
        assertTrue(both.contains(RetrievalIntent.CONCEPT_EXPLANATION));
        assertTrue(both.contains(RetrievalIntent.HARMONIC_EXAMPLE));
        assertEquals("D F# A C", computed("Explain V7/V in C major and give me a Mozart example."));
    }

    @Test
    void recognisesRequestsForPracticeAndProgress() {
        assertTrue(intents("Test me on secondary dominants.").contains(RetrievalIntent.EXERCISE_REQUEST));
        assertTrue(intents("How am I doing?").contains(RetrievalIntent.STUDENT_DIAGNOSTIC));
    }

    @Test
    @DisplayName("an unrecognised question asks for an explanation rather than nothing")
    void defaultsToExplaining() {
        assertEquals(Set.of(RetrievalIntent.CONCEPT_EXPLANATION), intents("hmm"));
        assertTrue(intents(null).isEmpty());
        assertTrue(intents("").isEmpty());
    }

    @Test
    @DisplayName("a computed answer records what was computed, so a response can say how it knew")
    void recordsTheOperation() {
        TheoryAnswer answer = TheoryQuestion.answer("What is V7/V in C major?").orElseThrow();

        assertEquals("realizeRomanNumeral(V7/V, C major)", answer.operation());
        assertEquals(TheoryAnswer.Kind.ROMAN_NUMERAL, answer.kind());
        assertTrue(answer.statement().contains("D F# A C"), answer.statement());
    }
}
