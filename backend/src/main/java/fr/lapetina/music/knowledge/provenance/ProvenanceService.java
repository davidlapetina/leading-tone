package fr.lapetina.music.knowledge.provenance;

import fr.lapetina.music.knowledge.attribution.Attribution;
import fr.lapetina.music.knowledge.harmony.MusicalExample;
import fr.lapetina.music.knowledge.retrieval.RetrievedChunk;
import fr.lapetina.music.knowledge.router.RetrievalIntent;
import fr.lapetina.music.knowledge.router.TutorKnowledge;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

/** Writes down what an answer was built from. */
@ApplicationScoped
public class ProvenanceService {

    private static final Logger LOG = Logger.getLogger(ProvenanceService.class);

    /**
     * Records one turn's evidence. Failing to record must never fail the turn: the learner
     * getting their answer matters more than the audit trail being complete.
     */
    @Transactional
    public void record(UUID sessionId, UUID interactionId, String conceptId, TutorKnowledge knowledge) {
        if (knowledge == null || knowledge.isEmpty()) {
            return;
        }
        try {
            ResponseProvenance provenance = new ResponseProvenance();
            provenance.sessionId = sessionId;
            provenance.interactionId = interactionId;
            provenance.conceptId = conceptId;
            provenance.intents = ResponseProvenance.join(
                    knowledge.intents().stream().map(RetrievalIntent::name).sorted().toList());
            provenance.chunkIds = ResponseProvenance.join(
                    knowledge.retrieved().stream().map(RetrievedChunk::chunkId).toList());
            // The row it came from, not the sentence it produced: a citation matched by its
            // printed text stops matching the moment the wording changes.
            provenance.harmonyEventIds = ResponseProvenance.join(
                    knowledge.examples().stream()
                            .map(example -> String.valueOf(example.eventId()))
                            .toList());
            provenance.theoryOperations = ResponseProvenance.join(knowledge.theoryOperations());
            provenance.sourceIds = ResponseProvenance.join(
                    knowledge.sources().stream().map(Attribution::sourceId).toList());
            provenance.persist();
        } catch (RuntimeException e) {
            LOG.warnf("Could not record provenance for interaction %s: %s", interactionId, e.toString());
        }
    }

    public List<ResponseProvenance> recent(int limit) {
        return ResponseProvenance.recent(Math.clamp(limit, 1, 200));
    }
}
