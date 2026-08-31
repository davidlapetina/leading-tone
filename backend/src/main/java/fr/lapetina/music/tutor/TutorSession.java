package fr.lapetina.music.tutor;

import fr.lapetina.music.learner.Learner;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tutor_session")
public class TutorSession extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    public Learner learner;

    @Column(name = "started_at", nullable = false)
    public Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    public Instant endedAt;
}
