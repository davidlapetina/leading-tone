package fr.lapetina.music.knowledge.ingestion;

import fr.lapetina.music.knowledge.source.SourceState;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One attempt at bringing a source in, kept so a failure can still be explained a week
 * later. A failed run never touches the generation that is currently serving.
 */
@Entity
@Table(name = "knowledge_ingestion_run")
public class IngestionRun extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @Column(name = "source_id", nullable = false)
    public String sourceId;

    @Column(nullable = false)
    public int generation;

    @Column(name = "started_at", nullable = false)
    public Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    public Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_state", nullable = false)
    public SourceState finalState = SourceState.DISCOVERED;

    @Column(name = "documents_seen", nullable = false)
    public int documentsSeen;

    @Column(name = "documents_ingested", nullable = false)
    public int documentsIngested;

    /** Refused because their own licence did not permit it. Counted, and named in the log. */
    @Column(name = "documents_skipped_license", nullable = false)
    public int documentsSkippedLicense;

    @Column(name = "documents_skipped_empty", nullable = false)
    public int documentsSkippedEmpty;

    @Column(name = "chunks_written", nullable = false)
    public int chunksWritten;

    @Column(name = "harmony_written", nullable = false)
    public int harmonyWritten;

    @Column(nullable = false)
    public boolean skipped = false;

    @Column(name = "embedding_model")
    public String embeddingModel;

    @Column
    public String fingerprint;

    @Column(length = 4000)
    public String message;

    public static IngestionRun latestFor(String sourceId) {
        return find("sourceId = ?1 order by startedAt desc", sourceId).firstResult();
    }
}
