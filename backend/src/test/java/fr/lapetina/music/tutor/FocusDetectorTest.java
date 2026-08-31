package fr.lapetina.music.tutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.concept.ConceptGraph;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FocusDetectorTest {

    private FocusDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FocusDetector();
        detector.conceptGraph = new ConceptGraph(new ObjectMapper());
    }

    private String conceptFor(String message) {
        return detector.detect(message).map(LearnerFocus::conceptId).orElse(null);
    }

    @Test
    @DisplayName("the question that broke the tutor is recognised")
    void recognisesTheQuestionFromTheTranscript() {
        assertEquals("seventh-chord", conceptFor("what is a C major add 7 chord"));
    }

    @Test
    void recognisesConceptsByNameAndByHowPeopleActuallySayThem() {
        assertEquals("chord-inversion", conceptFor("I still don't get inversions"));
        assertEquals("secondary-dominant", conceptFor("can you explain secondary dominants?"));
        assertEquals("cadence", conceptFor("what makes a plagal cadence different?"));
        assertEquals("key-signature", conceptFor("how many sharps does A major have"));
        assertEquals("mode", conceptFor("tell me about dorian"));
    }

    @Test
    @DisplayName("the longest phrase wins, so a dominant seventh is not just a dominant")
    void prefersTheMostSpecificMatch() {
        assertEquals("dominant-seventh", conceptFor("why does the dominant seventh want to resolve"));
        assertEquals("secondary-dominant", conceptFor("what is a secondary dominant"));
        assertEquals("dominant-function", conceptFor("what does dominant function mean"));
    }

    @Test
    void findsNothingWhenNothingWasAsked() {
        assertEquals(Optional.empty(), detector.detect("G7"));
        assertEquals(Optional.empty(), detector.detect("ok"));
        assertEquals(Optional.empty(), detector.detect(""));
        assertEquals(Optional.empty(), detector.detect(null));
    }

    @Test
    void keepsThePhraseTheLearnerUsed() {
        LearnerFocus focus = detector.detect("what is a C major add 7 chord").orElseThrow();
        assertTrue(focus.phrase().contains("add 7"), focus.phrase());
    }
}
