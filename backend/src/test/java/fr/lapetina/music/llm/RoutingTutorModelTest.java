package fr.lapetina.music.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoutingTutorModelTest {

    @Test
    @DisplayName("a tool call typed out as text is not a teacher's turn")
    void rejectsMarkupMasqueradingAsProse() {
        assertTrue(RoutingTutorModel.looksLikeMarkupRatherThanTeaching(
                "{\"name\": \"describeNote\", \"parameters\": {\"note\": \"Eb\"}}"));
        assertTrue(RoutingTutorModel.looksLikeMarkupRatherThanTeaching("[{\"tool\": \"analyzeChord\"}]"));
        assertTrue(RoutingTutorModel.looksLikeMarkupRatherThanTeaching("<tool_call>analyzeChord</tool_call>"));
        assertTrue(RoutingTutorModel.looksLikeMarkupRatherThanTeaching(
                "Let me check that. \"arguments\": {\"chord\": \"G7\"}"));
    }

    @Test
    void acceptsOrdinaryTeaching() {
        assertFalse(RoutingTutorModel.looksLikeMarkupRatherThanTeaching(
                "Write another name for Eb. There is one other note it can be called."));
        assertFalse(RoutingTutorModel.looksLikeMarkupRatherThanTeaching(
                "Play the dominant seventh of D major, and listen to what the C natural does."));
        assertFalse(RoutingTutorModel.looksLikeMarkupRatherThanTeaching(
                "\"Perfect authentic\" is the name for that one."));
    }
}
