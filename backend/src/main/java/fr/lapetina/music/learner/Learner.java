package fr.lapetina.music.learner;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import fr.lapetina.music.exercise.AnswerMode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learner")
public class Learner extends PanacheEntityBase {

    /**
     * V1 teaches one person, and that person has a fixed identity.
     *
     * <p>This is not cosmetic. "Find the learner, or create one" run twice at once
     * produces two learners, and then the interface reads one while it writes to the
     * other — which is exactly what a double-mounted React effect did. A constant primary
     * key makes the lookup idempotent and a duplicate impossible.
     */
    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-00006d757369");

    @Id
    public UUID id = UUID.randomUUID();

    @Column(name = "display_name", nullable = false)
    public String displayName;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Embedded
    public LearnerPreferences preferences = new LearnerPreferences();

    /**
     * How this learner has asked to practise, or null to let the tutor decide. An explicit
     * choice outranks the inferred preference — someone who says "let me play these" should
     * not have to keep saying it.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_answer_mode")
    public AnswerMode preferredAnswerMode;

    /**
     * A concept the learner has chosen to work on, or null to let the tutor choose. Free
     * mode: the learner still cannot mark their own work, but they can say what the
     * subject is.
     */
    @Column(name = "focus_concept_id")
    public String focusConceptId;

    /** A whole area — chords, harmony — to stay within, when no single concept was chosen. */
    @Column(name = "focus_category")
    public String focusCategory;

    public static Learner create(String displayName) {
        Learner learner = new Learner();
        learner.displayName = displayName;
        learner.persist();
        return learner;
    }

    /** Creates the one learner this installation teaches, at its fixed identity. */
    public static Learner createSingleton(String displayName) {
        Learner learner = new Learner();
        learner.id = SINGLETON_ID;
        learner.displayName = displayName;
        learner.persist();
        return learner;
    }
}
