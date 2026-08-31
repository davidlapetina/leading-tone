package fr.lapetina.music.learner;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One observation of the learner doing something, with the mastery figures before and
 * after. Append-only: this table is the audit trail for every claim the tutor makes.
 */
@Entity
@Table(name = "evidence")
public class Evidence extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    public Learner learner;

    @Column(name = "concept_id", nullable = false)
    public String conceptId;

    @Column(name = "session_id")
    public UUID sessionId;

    @Column(name = "interaction_id")
    public UUID interactionId;

    @Column(name = "exercise_id")
    public UUID exerciseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    public EvidenceType evidenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EvidenceResult result;

    @Column(nullable = false)
    public double correctness;

    /** How hard the question was, 0..1. Hard questions move mastery further. */
    @Column(nullable = false)
    public double difficulty;

    /** How sure the evaluator is about this observation, 0..1. */
    @Column(nullable = false)
    public double confidence;

    /** The combined multiplier actually applied, kept so past updates stay explainable. */
    @Column(nullable = false)
    public double weight;

    @Column(name = "mastery_before", nullable = false)
    public double masteryBefore;

    @Column(name = "mastery_after", nullable = false)
    public double masteryAfter;

    /** A short human-readable description of what produced this, e.g. "play V7 in D major". */
    @Column
    public String source;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
