package fr.lapetina.music.tutor;

import fr.lapetina.music.exercise.AnswerMode;
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

/** One turn of the conversation, with the pedagogical decision that produced it. */
@Entity
@Table(name = "interaction")
public class Interaction extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    public TutorSession session;

    @Column(nullable = false)
    public int sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public InteractionRole role;

    @Column(nullable = false, length = 8000)
    public String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "teaching_action")
    public TeachingAction teachingAction;

    /** Comma-separated concept ids this turn was aimed at. */
    @Column(name = "target_concepts")
    public String targetConcepts;

    @Column(name = "notation_abc", length = 4000)
    public String notationAbc;

    @Column(name = "expects_answer", nullable = false)
    public boolean expectsAnswer = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_mode")
    public AnswerMode answerMode;

    @Column(name = "exercise_id")
    public UUID exerciseId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
