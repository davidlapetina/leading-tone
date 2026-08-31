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

/** What the application currently believes about one learner and one concept. */
@Entity
@Table(name = "learner_concept")
public class LearnerConcept extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    public Learner learner;

    @Column(name = "concept_id", nullable = false)
    public String conceptId;

    @Column(nullable = false)
    public double mastery = 0.0;

    /** How much the evidence justifies the mastery figure, 0..1. */
    @Column(nullable = false)
    public double confidence = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public LearningState state = LearningState.UNKNOWN;

    @Column(name = "successful_evidence", nullable = false)
    public int successfulEvidence = 0;

    @Column(name = "failed_evidence", nullable = false)
    public int failedEvidence = 0;

    /** Failures since the last success, which is what decides whether to make the step smaller. */
    @Column(name = "consecutive_failures", nullable = false)
    public int consecutiveFailures = 0;

    /** Correct answers given through a high-weight channel, such as playing or explaining. */
    @Column(name = "strong_evidence", nullable = false)
    public int strongEvidence = 0;

    @Column(name = "last_practiced_at")
    public Instant lastPracticedAt;

    @Column(name = "next_review_at")
    public Instant nextReviewAt;

    @Column(name = "review_interval_days", nullable = false)
    public int reviewIntervalDays = 0;

    public int totalEvidence() {
        return successfulEvidence + failedEvidence;
    }

    public boolean isDue(Instant now) {
        return nextReviewAt != null && !nextReviewAt.isAfter(now);
    }

    public static LearnerConcept findOrCreate(Learner learner, String conceptId) {
        LearnerConcept existing = find("learner = ?1 and conceptId = ?2", learner, conceptId).firstResult();
        if (existing != null) {
            return existing;
        }
        LearnerConcept created = new LearnerConcept();
        created.learner = learner;
        created.conceptId = conceptId;
        created.persist();
        return created;
    }
}
