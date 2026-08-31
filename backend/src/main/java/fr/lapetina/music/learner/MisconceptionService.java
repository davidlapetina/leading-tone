package fr.lapetina.music.learner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Misconceptions are recorded only when a deterministic evaluator can name one. The
 * tutor may then decide to address it; it does not invent them.
 */
@ApplicationScoped
public class MisconceptionService {

    @Transactional
    public Misconception observe(Learner learner, String conceptId, String code, String description) {
        Misconception existing = Misconception
                .find("learner = ?1 and conceptId = ?2 and code = ?3", learner, conceptId, code)
                .firstResult();
        if (existing != null) {
            existing.occurrences++;
            existing.lastSeenAt = Instant.now();
            existing.resolvedAt = null;
            return existing;
        }
        Misconception created = new Misconception();
        created.learner = learner;
        created.conceptId = conceptId;
        created.code = code;
        created.description = description;
        created.persist();
        return created;
    }

    @Transactional
    public void resolve(Learner learner, String conceptId, String code) {
        Misconception.<Misconception>find("learner = ?1 and conceptId = ?2 and code = ?3", learner, conceptId, code)
                .firstResultOptional()
                .ifPresent(misconception -> misconception.resolvedAt = Instant.now());
    }

    public List<Misconception> open(Learner learner) {
        return Misconception.find("learner = ?1 and resolvedAt is null order by lastSeenAt desc", learner).list();
    }


    public List<Misconception> openFor(Learner learner, String conceptId) {
        return Misconception.find("learner = ?1 and conceptId = ?2 and resolvedAt is null", learner, conceptId).list();
    }
}
