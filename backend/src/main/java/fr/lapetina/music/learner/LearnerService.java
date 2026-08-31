package fr.lapetina.music.learner;

import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LearnerService {

    @Inject
    ConceptGraph conceptGraph;

    @Inject
    MisconceptionService misconceptionService;

    @Inject
    MasteryService masteryService;

    @ConfigProperty(name = "music.learner.default-name", defaultValue = "Student")
    String defaultName;

    /**
     * The learner this installation teaches, at a fixed identity so that two concurrent
     * requests cannot each decide to create one.
     */
    @Transactional
    public Learner current() {
        Learner existing = Learner.findById(Learner.SINGLETON_ID);
        return existing != null ? existing : Learner.createSingleton(defaultName);
    }

    /** Wipes everything known about the learner and starts them again from nothing. */
    @Transactional
    public Learner reset() {
        Learner.deleteAll();
        return Learner.createSingleton(defaultName);
    }

    public Optional<Learner> find(UUID id) {
        return Learner.<Learner>findByIdOptional(id);
    }

    @Transactional
    public Learner create(String displayName) {
        return Learner.create(displayName);
    }

    /** Records how the learner has asked to practise. Null hands the choice back to the tutor. */
    @Transactional
    public Learner choosePracticeMode(fr.lapetina.music.exercise.AnswerMode mode) {
        Learner learner = current();
        learner.preferredAnswerMode = mode;
        return learner;
    }

    /** Builds the full read model, including concepts the learner has never touched. */
    public LearnerSnapshot snapshot(Learner learner) {
        return snapshot(learner, Instant.now());
    }

    public LearnerSnapshot snapshot(Learner learner, Instant now) {
        Map<String, LearnerConcept> states = new LinkedHashMap<>();
        List<LearnerConcept> rows = LearnerConcept.find("learner", learner).list();
        for (LearnerConcept row : rows) {
            states.put(row.conceptId, row);
        }

        List<ConceptMastery> concepts = new ArrayList<>();
        List<ConceptMastery> due = new ArrayList<>();
        for (Concept concept : conceptGraph.all()) {
            LearnerConcept state = states.get(concept.id());
            // Derived now rather than trusted from the column: a concept becomes due for
            // review through the passage of time, not through anything that wrote a row.
            ConceptMastery mastery = state == null
                    ? ConceptMastery.unseen(concept)
                    : ConceptMastery.of(concept, state, masteryService.deriveState(state, now));
            concepts.add(mastery);
            if (state != null && state.isDue(now)) {
                due.add(mastery);
            }
        }

        List<MisconceptionView> misconceptions = misconceptionService.open(learner).stream()
                .map(MisconceptionView::of)
                .toList();

        return new LearnerSnapshot(learner.id, learner.displayName, List.copyOf(concepts), List.copyOf(due),
                misconceptions, preferencesOf(learner), learner.preferredAnswerMode);
    }

    public Map<String, Double> preferencesOf(Learner learner) {
        LearnerPreferences preferences = learner.preferences;
        return Map.of(
                "explanationDepth", round(preferences.explanationDepth),
                "socraticPreference", round(preferences.socraticPreference),
                "notationPreference", round(preferences.notationPreference),
                "keyboardPreference", round(preferences.keyboardPreference),
                "auralPreference", round(preferences.auralPreference),
                "examplePreference", round(preferences.examplePreference),
                "abstractionTolerance", round(preferences.abstractionTolerance));
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
