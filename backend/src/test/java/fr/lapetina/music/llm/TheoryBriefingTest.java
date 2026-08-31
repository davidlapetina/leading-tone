package fr.lapetina.music.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The briefing is what stops the model inventing theory, so it has to be right. Every
 * fact below is computed by the engine, and these tests pin the ones that matter.
 */
class TheoryBriefingTest {

    private final TheoryBriefing briefing = new TheoryBriefing();

    @Test
    @DisplayName("a C major seventh is C E G B, and is not confused with C7")
    void spellsSeventhsCorrectly() {
        String facts = briefing.forConcept("seventh-chord");
        assertTrue(facts.contains("Cmaj7"), facts);
        assertTrue(facts.contains("C E G B\n") || facts.contains("C E G B."), facts);
        assertTrue(facts.contains("C E G Bb"), facts);
        assertTrue(facts.contains("add 7"), facts);
        assertTrue(facts.contains("different chord"), facts);
    }

    @Test
    void spellsScalesAndSignaturesFromTheEngine() {
        String facts = briefing.forConcept("major-scale");
        assertTrue(facts.contains("D E F# G A B C#"), facts);
        assertTrue(facts.contains("Eb F G Ab Bb C D"), facts);
        assertTrue(facts.contains("2 sharps"), facts);
        assertTrue(facts.contains("3 flats"), facts);
    }

    @Test
    void getsInversionsAndTheirFiguresRight() {
        String facts = briefing.forConcept("chord-inversion");
        assertTrue(facts.contains("B D G"), facts);
        assertTrue(facts.contains("D G B"), facts);
        assertTrue(facts.contains("lowest note"), facts);
    }

    @Test
    void namesTheDominantSeventhOfEachKey() {
        String facts = briefing.forConcept("dominant-seventh");
        assertTrue(facts.contains("G7"), facts);
        assertTrue(facts.contains("A7"), facts);
        assertTrue(facts.contains("E7"), facts);
    }

    @Test
    void agreesWithTheAnalyserAboutCadences() {
        String facts = briefing.forConcept("cadence");
        assertTrue(facts.contains("perfect authentic cadence"), facts);
        assertTrue(facts.contains("deceptive cadence"), facts);
    }

    @Test
    void tellsTheModelNotToWorkItOutItself() {
        assertTrue(briefing.forConcept("triad").contains("do not"));
    }

    @Test
    @DisplayName("no concept produces a briefing that throws or lies about being present")
    void coversTheGraphWithoutFailing() {
        ConceptGraph graph = new ConceptGraph(new ObjectMapper());
        for (Concept concept : graph.all()) {
            String facts = briefing.forConcept(concept.id());
            assertFalse(facts.contains("null"), concept.id() + ": " + facts);
        }
    }
}
