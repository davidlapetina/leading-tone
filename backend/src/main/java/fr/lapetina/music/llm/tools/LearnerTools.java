package fr.lapetina.music.llm.tools;

import dev.langchain4j.agent.tool.Tool;
import fr.lapetina.music.concept.ConceptGraph;
import fr.lapetina.music.learner.ConceptMastery;
import fr.lapetina.music.learner.EvidenceObservation;
import fr.lapetina.music.learner.EvidenceResult;
import fr.lapetina.music.learner.EvidenceService;
import fr.lapetina.music.learner.EvidenceType;
import fr.lapetina.music.learner.Learner;
import fr.lapetina.music.learner.LearnerService;
import fr.lapetina.music.learner.LearnerSnapshot;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Set;
import org.jboss.logging.Logger;

/**
 * What the language model may know and say about the learner.
 *
 * <p>There is a {@code proposeEvidence} and there is no {@code setMastery}. The model
 * can report what it saw; the weight that observation carries, and what it does to the
 * learner model, are decided here — and so is whether the report is accepted at all. See
 * {@link TurnScope} for why that fence exists.
 */
@ApplicationScoped
public class LearnerTools {

    private static final Logger LOG = Logger.getLogger(LearnerTools.class);

    @Inject
    LearnerService learnerService;

    @Inject
    EvidenceService evidenceService;

    @Inject
    ConceptGraph conceptGraph;

    @Inject
    TurnScope turnScope;

    /**
     * The only kinds of evidence a model could actually have witnessed. It cannot have
     * watched anyone play a chord, so it may not claim to have.
     */
    private static final Set<EvidenceType> PROPOSABLE =
            Set.of(EvidenceType.EXPLANATION, EvidenceType.SELF_EXPLANATION, EvidenceType.TEXT_RECALL);

    @Tool("Read what the tutor currently believes the learner knows. Read-only.")
    public String getLearnerState() {
        Learner learner = learnerService.current();
        LearnerSnapshot snapshot = learnerService.snapshot(learner);
        StringBuilder builder = new StringBuilder();
        for (ConceptMastery concept : snapshot.concepts()) {
            if (concept.state() != fr.lapetina.music.learner.LearningState.UNKNOWN) {
                builder.append("- %s: mastery %.2f, confidence %.2f, %s%n".formatted(
                        concept.conceptId(), concept.mastery(), concept.confidence(), concept.state()));
            }
        }
        return builder.isEmpty() ? "Nothing has been observed about this learner yet." : builder.toString();
    }

    @Tool("""
            Report something you observed the learner do or say, so it can be recorded. \
            conceptId must be a known concept. evidenceType is usually EXPLANATION or \
            SELF_EXPLANATION. result must be CORRECT, PARTIALLY_CORRECT or INCORRECT. \
            This is a proposal: the tutoring engine decides what it is worth, and you cannot \
            set mastery yourself.""")
    public String proposeEvidence(String conceptId, String evidenceType, String result, String reason) {
        if (!conceptGraph.contains(conceptId)) {
            return "There is no concept called '%s'.".formatted(conceptId);
        }
        if (!turnScope.isActive()) {
            return "Evidence can only be proposed while teaching a concept.";
        }
        if (!turnScope.allows(conceptId)) {
            return ("Only one proposal per turn is accepted, and only about %s, which is the concept "
                    + "being taught right now.").formatted(turnScope.conceptId());
        }
        EvidenceType type;
        EvidenceResult verdict;
        try {
            type = EvidenceType.valueOf(evidenceType.trim().toUpperCase());
            verdict = EvidenceResult.valueOf(result.trim().toUpperCase());
        } catch (IllegalArgumentException unknownValue) {
            return "Unrecognised evidence type or result: " + unknownValue.getMessage();
        }
        if (!PROPOSABLE.contains(type)) {
            return ("You can only report what a learner said, not what they played. "
                    + "Use EXPLANATION, SELF_EXPLANATION or TEXT_RECALL.");
        }
        turnScope.markUsed();

        Learner learner = learnerService.current();
        EvidenceObservation observation = new EvidenceObservation(conceptId, type, verdict,
                conceptGraph.require(conceptId).intrinsicDifficulty(),
                EvidenceService.MODEL_JUDGED_CONFIDENCE, reason, null, null, null);
        var evidence = evidenceService.record(learner, observation);
        LOG.infof("Model-proposed evidence for %s accepted at confidence %.2f",
                conceptId, EvidenceService.MODEL_JUDGED_CONFIDENCE);
        return "Recorded as %s for %s at confidence %.2f. Mastery moved from %.2f to %.2f."
                .formatted(verdict, conceptId, EvidenceService.MODEL_JUDGED_CONFIDENCE,
                        evidence.masteryBefore, evidence.masteryAfter);
    }
}
