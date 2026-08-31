package fr.lapetina.music.exercise;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exercise_attempt")
public class ExerciseAttempt extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @Column(name = "exercise_id", nullable = false)
    public UUID exerciseId;

    @Column(name = "raw_answer", nullable = false, length = 2000)
    public String rawAnswer;

    @Column(nullable = false)
    public boolean correct;

    @Column(nullable = false)
    public boolean partial;

    @Column(length = 2000)
    public String feedback;

    /** Structured detail from the evaluator, kept as JSON for later inspection. */
    @Column(length = 4000)
    public String detail;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
