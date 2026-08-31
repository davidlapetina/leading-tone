package fr.lapetina.music.tutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.learner.ConceptMastery;
import fr.lapetina.music.learner.LearnerSnapshot;
import fr.lapetina.music.learner.LearningState;
import fr.lapetina.music.learner.MisconceptionView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The policy is the part of the tutor that decides what happens next, so it is tested
 * without a language model anywhere in sight.
 */
class TeachingPolicyTest {

    private ConceptGraph graph;
    private TeachingPolicy policy;

    @BeforeEach
    void setUp() {
        graph = new ConceptGraph(new ObjectMapper());
        policy = new TeachingPolicy();
        policy.conceptGraph = graph;
    }

    private LearnerSnapshot snapshot(Map<String, Double> masteries, List<String> due,
                                     List<MisconceptionView> misconceptions) {
        List<ConceptMastery> concepts = new ArrayList<>();
        List<ConceptMastery> dueList = new ArrayList<>();
        for (Concept concept : graph.all()) {
            double mastery = masteries.getOrDefault(concept.id(), 0.0);
            ConceptMastery view = new ConceptMastery(concept.id(), concept.name(), concept.category().name(),
                    mastery, mastery > 0 ? 0.9 : 0.0, stateFor(mastery), mastery > 0 ? 5 : 0, 0, 0,
                    mastery > 0 ? Instant.now() : null, null);
            concepts.add(view);
            if (due.contains(concept.id())) {
                dueList.add(view);
            }
        }
        return snapshot(masteries, due, misconceptions, null, null);
    }

    private LearnerSnapshot snapshot(Map<String, Double> masteries, List<String> due,
                                     List<MisconceptionView> misconceptions,
                                     String focusConcept, String focusCategory) {
        List<ConceptMastery> concepts = new ArrayList<>();
        List<ConceptMastery> dueList = new ArrayList<>();
        for (Concept concept : graph.all()) {
            double mastery = masteries.getOrDefault(concept.id(), 0.0);
            ConceptMastery view = new ConceptMastery(concept.id(), concept.name(), concept.category().name(),
                    mastery, mastery > 0 ? 0.9 : 0.0, stateFor(mastery), mastery > 0 ? 5 : 0, 0, 0,
                    mastery > 0 ? Instant.now() : null, null);
            concepts.add(view);
            if (due.contains(concept.id())) {
                dueList.add(view);
            }
        }
        return new LearnerSnapshot(UUID.randomUUID(), "Test", concepts, dueList, misconceptions,
                Map.of("keyboardPreference", 0.9), null, focusConcept, focusCategory);
    }

    private static LearningState stateFor(double mastery) {
        if (mastery == 0.0) {
            return LearningState.UNKNOWN;
        }
        if (mastery < 0.15) {
            return LearningState.INTRODUCED;
        }
        if (mastery < 0.45) {
            return LearningState.LEARNING;
        }
        if (mastery < 0.70) {
            return LearningState.PRACTICING;
        }
        return mastery < 0.88 ? LearningState.RELIABLE : LearningState.MASTERED;
    }

    private Map<String, Double> allAt(double mastery) {
        Map<String, Double> masteries = new HashMap<>();
        graph.all().forEach(concept -> masteries.put(concept.id(), mastery));
        return masteries;
    }

    @Test
    @DisplayName("an empty learner model produces a diagnosis, not a lesson one")
    void startsByFindingOut() {
        TeachingDecision decision = policy.next(snapshot(Map.of(), List.of(), List.of()));
        assertEquals(TeachingAction.DIAGNOSE, decision.action());
        assertEquals("note", decision.conceptId());
    }

    @Test
    void aRepeatedMisconceptionInterruptsEverythingElse() {
        Map<String, Double> masteries = allAt(0.5);
        MisconceptionView misconception = new MisconceptionView("chord-inversion",
                "plays-root-position-when-inversion-asked", "Defaults to root position.", 3, Instant.now());
        TeachingDecision decision = policy.next(
                snapshot(masteries, List.of("triad"), List.of(misconception)));
        assertEquals(TeachingAction.CORRECT_MISCONCEPTION, decision.action());
        assertEquals("chord-inversion", decision.conceptId());
    }

    @Test
    @DisplayName("a misconception seen once is noted but not acted on")
    void aSingleSlipIsNotAMisconception() {
        MisconceptionView misconception = new MisconceptionView("chord-inversion",
                "plays-root-position-when-inversion-asked", "Defaults to root position.", 1, Instant.now());
        TeachingDecision decision = policy.next(snapshot(allAt(0.5), List.of(), List.of(misconception)));
        assertTrue(decision.action() != TeachingAction.CORRECT_MISCONCEPTION);
    }

    @Test
    void reviewComesBeforeNewGround() {
        TeachingDecision decision = policy.next(snapshot(allAt(0.5), List.of("interval"), List.of()));
        assertEquals(TeachingAction.REVIEW, decision.action());
        assertEquals("interval", decision.conceptId());
    }

    @Test
    void introducesTheNextThingWhoseGroundworkIsSolid() {
        Map<String, Double> masteries = allAt(0.9);
        masteries.put("modulation", 0.0);
        TeachingDecision decision = policy.next(snapshot(masteries, List.of(), List.of()));
        assertEquals(TeachingAction.INTRODUCE, decision.action());
        assertEquals("modulation", decision.conceptId());
    }

    @Test
    @DisplayName("something half-learned is finished before something new is started")
    void finishesWhatWasStarted() {
        Map<String, Double> masteries = allAt(0.9);
        masteries.put("triad", 0.30);
        masteries.put("modulation", 0.0);
        TeachingDecision decision = policy.next(snapshot(masteries, List.of(), List.of()));
        assertEquals("triad", decision.conceptId());
        assertEquals(TeachingAction.PRACTICE, decision.action());
    }

    @Test
    void explainsAgainWhenSomethingHasBarelyLanded() {
        Map<String, Double> masteries = allAt(0.9);
        masteries.put("triad", 0.18);
        TeachingDecision decision = policy.next(snapshot(masteries, List.of(), List.of()));
        assertEquals("triad", decision.conceptId());
        assertEquals(TeachingAction.EXPLAIN, decision.action());
    }

    @Test
    @DisplayName("nothing new is built on a prerequisite that is only just known")
    void goesBackWhenTheGroundworkIsShaky() {
        Map<String, Double> masteries = allAt(0.9);
        masteries.put("seventh-chord", 0.50);
        masteries.put("dominant-seventh", 0.0);
        TeachingDecision decision = policy.next(snapshot(masteries, List.of(), List.of()));
        assertEquals(TeachingAction.REINFORCE, decision.action());
        assertEquals("seventh-chord", decision.conceptId());
        assertTrue(decision.rationale().contains("dominant-seventh"));
    }

    @Test
    void pushesHarderOnceEverythingIsHeld() {
        assertEquals(TeachingAction.CHALLENGE, policy.next(snapshot(allAt(0.75), List.of(), List.of())).action());
        assertEquals(TeachingAction.TRANSFER, policy.next(snapshot(allAt(0.92), List.of(), List.of())).action());
    }

    @Test
    void difficultyTracksMasteryAndTheConceptItself() {
        Map<String, Double> low = allAt(0.9);
        low.put("triad", 0.2);
        double easyTarget = policy.next(snapshot(low, List.of(), List.of())).difficulty();

        Map<String, Double> high = allAt(0.9);
        high.put("triad", 0.7);
        double harderTarget = policy.next(snapshot(high, List.of(), List.of())).difficulty();

        assertTrue(harderTarget > easyTarget, easyTarget + " should be below " + harderTarget);
        assertTrue(easyTarget >= 0.15 && harderTarget <= 0.95);
    }

    @Test
    @DisplayName("every action the policy can produce ends in something to answer")
    void neverProducesATurnThatCannotGenerateEvidence() {
        // A turn with nothing to answer produces no evidence, and a concept with no
        // evidence can never move. That is how the tutor deadlocked.
        Map<String, Double> justIntroduced = allAt(0.9);
        justIntroduced.put("triad", 0.05);
        for (TeachingDecision decision : List.of(
                policy.next(snapshot(Map.of(), List.of(), List.of())),
                policy.next(snapshot(justIntroduced, List.of(), List.of())),
                policy.next(snapshot(allAt(0.5), List.of("interval"), List.of())),
                policy.next(snapshot(allAt(0.75), List.of(), List.of())),
                policy.next(snapshot(allAt(0.92), List.of(), List.of())))) {
            assertTrue(decision.expectsAnswer(),
                    decision.action() + " produced a turn with nothing to answer");
            assertNotEquals(AnswerMode.NONE, decision.preferredAnswerMode());
        }
    }

    @Test
    @DisplayName("what the learner asked about beats what the tutor had planned")
    void takesUpTheLearnersQuestion() {
        // Left alone, the tutor would introduce modulation. The learner asks about
        // cadences instead, and everything cadences rest on is already in place.
        Map<String, Double> masteries = allAt(0.9);
        masteries.put("modulation", 0.0);
        assertEquals("modulation", policy.next(snapshot(masteries, List.of(), List.of())).conceptId());

        TeachingDecision decision = policy.next(
                snapshot(masteries, List.of(), List.of()),
                Optional.of(new LearnerFocus("cadence", "cadence")));

        assertEquals(TeachingAction.ANSWER_QUESTION, decision.action());
        assertEquals("cadence", decision.conceptId());
        assertEquals("cadence", decision.learnerAskedAbout());
    }

    @Test
    @DisplayName("a question about something out of reach is acknowledged, then grounded")
    void answersOutOfReachQuestionsByGoingBack() {
        Map<String, Double> masteries = allAt(0.0);
        TeachingDecision decision = policy.next(
                snapshot(masteries, List.of(), List.of()),
                Optional.of(new LearnerFocus("secondary-dominant", "secondary dominant")));

        assertEquals(TeachingAction.REINFORCE, decision.action());
        assertEquals("secondary-dominant", decision.learnerAskedAbout());
        assertNotEquals("secondary-dominant", decision.conceptId());
        assertTrue(decision.rationale().contains("secondary-dominant"), decision.rationale());
    }

    @Test
    @DisplayName("free mode: a chosen concept is taught, whatever else was due")
    void teachesTheConceptTheLearnerChose() {
        Map<String, Double> masteries = allAt(0.9);
        masteries.put("triad", 0.3);
        TeachingDecision guided = policy.next(snapshot(masteries, List.of("interval"), List.of()));
        assertEquals("interval", guided.conceptId());

        TeachingDecision chosen = policy.next(
                snapshot(masteries, List.of("interval"), List.of(), "cadence", null));
        assertEquals("cadence", chosen.conceptId());
    }

    @Test
    @DisplayName("a chosen area narrows the frontier without abandoning prerequisites")
    void staysWithinTheChosenArea() {
        Map<String, Double> masteries = allAt(0.9);
        graph.all().stream()
                .filter(concept -> concept.category().name().equals("HARMONY"))
                .forEach(concept -> masteries.put(concept.id(), 0.0));

        TeachingDecision decision = policy.next(
                snapshot(masteries, List.of(), List.of(), null, "HARMONY"));
        assertEquals("HARMONY", graph.require(decision.conceptId()).category().name());
    }

    @Test
    @DisplayName("a learner who answers well at the keyboard gets asked to play")
    void picksTheChannelTheLearnerAnswersBestOn() {
        Map<String, Double> masteries = allAt(0.9);
        masteries.put("triad", 0.3);
        assertEquals(AnswerMode.MIDI, policy.next(snapshot(masteries, List.of(), List.of())).preferredAnswerMode());
    }
}
