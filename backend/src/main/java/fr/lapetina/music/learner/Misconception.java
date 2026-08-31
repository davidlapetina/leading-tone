package fr.lapetina.music.learner;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A specific wrong belief, detected deterministically rather than guessed at: playing
 * every chord in root position, or forgetting to raise the leading tone in minor.
 */
@Entity
@Table(name = "misconception")
public class Misconception extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    public Learner learner;

    @Column(name = "concept_id", nullable = false)
    public String conceptId;

    /** Stable identifier such as {@code plays-root-position-when-inversion-asked}. */
    @Column(nullable = false)
    public String code;

    @Column(nullable = false)
    public String description;

    @Column(nullable = false)
    public int occurrences = 1;

    @Column(name = "detected_at", nullable = false)
    public Instant detectedAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    public Instant lastSeenAt = Instant.now();

    @Column(name = "resolved_at")
    public Instant resolvedAt;

    public boolean isOpen() {
        return resolvedAt == null;
    }
}
