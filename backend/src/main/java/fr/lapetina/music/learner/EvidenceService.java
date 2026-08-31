package fr.lapetina.music.learner;

import fr.lapetina.music.concept.ConceptGraph;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * The single writer of the learner model.
 *
 * <p>Everything that could change what the application believes goes through here:
 * evidence is persisted first, then mastery is recomputed from it, then the review
 * schedule follows. Nothing else in the codebase assigns to {@code mastery}.
 */
@ApplicationScoped
public class EvidenceService {

    private static final Logger LOG = Logger.getLogger(EvidenceService.class);

    /**
     * The ceiling on evidence whose verdict came from the language model rather than
     * from a deterministic check. A model's opinion is worth recording, but it is not
     * worth as much as watching someone play the chord.
     */
    public static final double MODEL_JUDGED_CONFIDENCE = 0.6;

    @Inject
    MasteryService masteryService;

    @Inject
    ReviewScheduler reviewScheduler;

    @Inject
    ConceptGraph conceptGraph;

    @Transactional
    public Evidence record(Learner learner, EvidenceObservation observation) {
        return record(learner, observation, Instant.now());
    }

    @Transactional
    public Evidence record(Learner learner, EvidenceObservation observation, Instant now) {
        if (!conceptGraph.contains(observation.conceptId())) {
            throw new IllegalArgumentException("Unknown concept: " + observation.conceptId());
        }
        LearnerConcept target = LearnerConcept.findOrCreate(learner, observation.conceptId());
        double difficulty = observation.difficulty() > 0
                ? observation.difficulty()
                : conceptGraph.require(observation.conceptId()).intrinsicDifficulty();

        MasteryUpdate update = masteryService.apply(target, observation.type(), observation.result(),
                difficulty, observation.confidence(), now);
        reviewScheduler.schedule(target, observation.result().isPositive(), now);
        target.state = masteryService.deriveState(target, now);

        Evidence evidence = new Evidence();
        evidence.learner = learner;
        evidence.conceptId = observation.conceptId();
        evidence.sessionId = observation.sessionId();
        evidence.interactionId = observation.interactionId();
        evidence.exerciseId = observation.exerciseId();
        evidence.evidenceType = observation.type();
        evidence.result = observation.result();
        evidence.correctness = observation.result().correctness();
        evidence.difficulty = difficulty;
        evidence.confidence = observation.confidence();
        evidence.weight = update.weight();
        evidence.masteryBefore = update.masteryBefore();
        evidence.masteryAfter = update.masteryAfter();
        evidence.source = observation.source();
        evidence.createdAt = now;
        evidence.persist();

        updatePreferences(learner, observation);

        LOG.debugf("%s %s: %.3f -> %.3f (%s)", observation.conceptId(), observation.result(),
                update.masteryBefore(), update.masteryAfter(), update.stateAfter());
        return evidence;
    }

    @Transactional
    public List<Evidence> recordAll(Learner learner, List<EvidenceObservation> observations) {
        Instant now = Instant.now();
        List<Evidence> recorded = new ArrayList<>(observations.size());
        for (EvidenceObservation observation : observations) {
            recorded.add(record(learner, observation, now));
        }
        return recorded;
    }

    /**
     * Learning-style preferences drift towards whatever the learner actually succeeds at,
     * rather than towards what they say they like.
     */
    private void updatePreferences(Learner learner, EvidenceObservation observation) {
        boolean worked = observation.result().isPositive();
        LearnerPreferences preferences = learner.preferences;
        switch (observation.type().channel()) {
            case MIDI -> preferences.keyboardPreference =
                    LearnerPreferences.nudge(preferences.keyboardPreference, worked);
            case TEXT -> preferences.notationPreference =
                    LearnerPreferences.nudge(preferences.notationPreference, worked);
        }
        if (observation.type() == EvidenceType.EXPLANATION || observation.type() == EvidenceType.SELF_EXPLANATION) {
            preferences.abstractionTolerance =
                    LearnerPreferences.nudge(preferences.abstractionTolerance, worked);
        }
        if (observation.type() == EvidenceType.AURAL_RECOGNITION) {
            preferences.auralPreference = LearnerPreferences.nudge(preferences.auralPreference, worked);
        }
    }

    public List<Evidence> history(Learner learner, String conceptId, int limit) {
        return Evidence.find("learner = ?1 and conceptId = ?2 order by createdAt desc", learner, conceptId)
                .page(0, limit).list();
    }

    public List<Evidence> recent(Learner learner, int limit) {
        return Evidence.find("learner = ?1 order by createdAt desc", learner).page(0, limit).list();
    }
}
