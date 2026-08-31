package fr.lapetina.music.exercise;

import fr.lapetina.music.learner.EvidenceType;
import fr.lapetina.music.learner.Learner;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exercise")
public class Exercise extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    public Learner learner;

    @Column(name = "session_id")
    public UUID sessionId;

    @Column(name = "concept_id", nullable = false)
    public String conceptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false)
    public ExerciseType exerciseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_mode", nullable = false)
    public AnswerMode answerMode;

    /** What the learner was asked to do: recognise it, build it, or explain it in context. */
    @Enumerated(EnumType.STRING)
    @Column(name = "task_kind", nullable = false)
    public TaskKind taskKind = TaskKind.IDENTIFY;

    @Column(nullable = false, length = 2000)
    public String prompt;

    /** The {@link ExpectedAnswer} as JSON, so an attempt can be judged later. */
    @Column(name = "expected_answer", nullable = false, length = 4000)
    public String expectedAnswer;

    @Column(name = "key_context")
    public String keyContext;

    @Column(nullable = false)
    public double difficulty;

    @Column(name = "notation_abc", length = 4000)
    public String notationAbc;

    /** What help the question came with, so a right answer is weighted for it. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Scaffold scaffold = Scaffold.NONE;

    /** The options offered, when the question was narrowed to a choice. Newline separated. */
    @Column(length = 2000)
    public String choices;

    @Column(nullable = false)
    public boolean solved = false;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    /** Not persisted: the evidence type is derived from the exercise's answer mode and kind. */
    @Transient
    public EvidenceType evidenceType;
}
