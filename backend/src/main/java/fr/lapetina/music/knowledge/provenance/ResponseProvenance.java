package fr.lapetina.music.knowledge.provenance;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What went into one answer.
 *
 * <p>Recorded so a claim in a lesson can be traced back afterwards: which passages were
 * quoted, which annotated bars were cited, what the theory engine computed, and which
 * sources were credited. Without this, "the tutor said Beethoven does this" is unfalsifiable
 * a day later.
 *
 * <p>Deliberately <strong>not</strong> the model's reasoning. That is neither observable nor
 * ours to keep, and storing a plausible-looking rationalisation next to real evidence would
 * make the evidence harder to trust rather than easier.
 */
@Entity
@Table(name = "response_provenance")
public class ResponseProvenance extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @Column(name = "interaction_id")
    public UUID interactionId;

    @Column(name = "session_id")
    public UUID sessionId;

    @Column(name = "concept_id")
    public String conceptId;

    /** The intents the question was routed on, comma separated. */
    @Column(length = 500)
    public String intents;

    @Column(name = "chunk_ids", length = 4000)
    public String chunkIds;

    @Column(name = "harmony_event_ids", length = 4000)
    public String harmonyEventIds;

    @Column(name = "theory_operations", length = 4000)
    public String theoryOperations;

    @Column(name = "source_ids", length = 2000)
    public String sourceIds;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    public static List<ResponseProvenance> recent(int limit) {
        return find("order by createdAt desc").page(0, limit).list();
    }

    static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }
}
