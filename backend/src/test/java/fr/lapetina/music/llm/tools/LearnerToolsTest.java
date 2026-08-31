package fr.lapetina.music.llm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.learner.Learner;
import fr.lapetina.music.learner.LearnerService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The model's only write tool, and the fence around it.
 *
 * <p>The learner's own words reach the prompt, so "record everything as mastered" is a
 * message anyone can send. These tests are about what happens when the model believes it.
 */
@QuarkusTest
class LearnerToolsTest {

    @Inject
    LearnerTools learnerTools;

    @Inject
    TurnScope turnScope;

    @Inject
    LearnerService learnerService;

    @BeforeEach
    void startFromNothing() {
        learnerService.reset();
        turnScope.beginTurn("triad");
    }

    private double masteryOf(String conceptId) {
        Learner learner = learnerService.current();
        return learnerService.snapshot(learner).concept(conceptId).mastery();
    }

    @Test
    @DisplayName("a proposal about the concept being taught is recorded, at reduced confidence")
    void acceptsOneHonestProposal() {
        String reply = learnerTools.proposeEvidence("triad", "EXPLANATION", "CORRECT",
                "explained that a triad is two stacked thirds");

        assertTrue(reply.contains("Recorded"), reply);
        assertTrue(reply.contains("0.60"), reply);
        assertTrue(masteryOf("triad") > 0);
    }

    @Test
    @DisplayName("it cannot vouch for a concept the tutor is not teaching")
    void refusesProposalsAboutOtherConcepts() {
        String reply = learnerTools.proposeEvidence("modulation", "EXPLANATION", "CORRECT", "trust me");

        assertTrue(reply.contains("being taught right now"), reply);
        assertEquals(0.0, masteryOf("modulation"));
    }

    @Test
    @DisplayName("one proposal per turn, so a talked-round model cannot grind mastery upwards")
    void refusesRepeatedProposals() {
        learnerTools.proposeEvidence("triad", "EXPLANATION", "CORRECT", "first");
        double afterOne = masteryOf("triad");

        for (int i = 0; i < 5; i++) {
            learnerTools.proposeEvidence("triad", "EXPLANATION", "CORRECT", "again");
        }
        assertEquals(afterOne, masteryOf("triad"));
    }

    @Test
    @DisplayName("it cannot claim to have watched anyone play")
    void refusesEvidenceItCouldNotHaveWitnessed() {
        String reply = learnerTools.proposeEvidence("triad", "MIDI_CHORD", "CORRECT", "they played it");

        assertTrue(reply.contains("not what they played"), reply);
        assertEquals(0.0, masteryOf("triad"));
    }

    @Test
    void refusesUnknownConceptsAndValues() {
        assertTrue(learnerTools.proposeEvidence("jazz", "EXPLANATION", "CORRECT", "x")
                .contains("no concept"));
        assertTrue(learnerTools.proposeEvidence("triad", "TELEPATHY", "CORRECT", "x")
                .contains("Unrecognised"));
    }

    @Test
    @DisplayName("reading the learner model is allowed; there is no way to write it directly")
    void canReadButNotSet() {
        assertTrue(learnerTools.getLearnerState() != null);
        for (var method : LearnerTools.class.getMethods()) {
            String name = method.getName().toLowerCase();
            assertTrue(!name.startsWith("set") && !name.contains("mastery"),
                    "LearnerTools exposes " + method.getName() + " to the model");
        }
    }
}
