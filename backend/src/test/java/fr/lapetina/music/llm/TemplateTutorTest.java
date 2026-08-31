package fr.lapetina.music.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.learner.LearnerSnapshot;
import fr.lapetina.music.tutor.TeachingAction;
import fr.lapetina.music.tutor.TeachingDecision;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The tutor with no model. It says the connecting words, and it must never say anything
 * that was written for the model rather than for the learner.
 */
class TemplateTutorTest {

    private final TemplateTutor tutor = new TemplateTutor();

    private static TutorRequest request(String learnerFeedback, String modelDirective) {
        LearnerSnapshot snapshot = new LearnerSnapshot(UUID.randomUUID(), "Test", List.of(), List.of(),
                List.of(), Map.of(), null, null, null);
        TeachingDecision decision = new TeachingDecision(TeachingAction.PRACTICE, "triad", "Triads",
                List.of(), 0.5, AnswerMode.TEXT, "because", null);
        return new TutorRequest(UUID.randomUUID(), snapshot, decision, null, "F Ab C",
                learnerFeedback, modelDirective);
    }

    @Test
    @DisplayName("the learner hears the verdict, never the instructions written for the model")
    void neverReadsTheModelsInstructionsAloud() {
        String directive = "They answered the previous question and it was correct. "
                + "Acknowledge that in a few words and do not restate anything.";
        String said = tutor.respond(request("Expected Eb.", directive));

        assertTrue(said.contains("Expected Eb."), said);
        assertFalse(said.contains("Acknowledge"), said);
        assertFalse(said.contains("do not restate"), said);
        assertFalse(said.contains("previous question"), said);
    }

    @Test
    void saysSomethingUsableWithNoFeedbackAtAll() {
        String said = tutor.respond(request(null, null));
        assertFalse(said.isBlank());
        assertTrue(said.toLowerCase().contains("triad"), said);
    }
}
