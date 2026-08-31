package fr.lapetina.music.knowledge.source;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

/**
 * What has happened to one declared source on this machine.
 *
 * <p>The split matters: {@code knowledge-sources.yaml} declares what a source <em>is</em>
 * and on whose terms, and that can only change by editing a reviewed file. This table
 * records only what was done with it here — downloaded when, parsed by which parser,
 * indexed into which generation. The database can never grant a permission the manifest
 * withholds.
 */
@Entity
@Table(name = "knowledge_source")
public class KnowledgeSource extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public String id;

    @Column(name = "display_name", nullable = false)
    public String displayName;

    @Column(name = "license_id", nullable = false)
    public String licenseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_status", nullable = false)
    public fr.lapetina.music.knowledge.license.LicenseStatus licenseStatus =
            fr.lapetina.music.knowledge.license.LicenseStatus.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_mode", nullable = false)
    public IngestionMode ingestionMode = IngestionMode.TEXT_RAG;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    public SourceState state = SourceState.DISCOVERED;

    /** Declared in the manifest. A disabled source is never ingested and never retrieved. */
    @Column(name = "enabled", nullable = false)
    public boolean enabled = true;

    @Column(name = "source_version")
    public String sourceVersion;

    @Column(name = "source_commit")
    public String sourceCommit;

    /**
     * Covers source version, parser, chunk policy and embedding model. Re-running an
     * ingest whose fingerprint is unchanged is a no-op, which is what makes ingestion
     * idempotent; changing the embedding model changes the fingerprint, which is what
     * forces a rebuild instead of mixing vectors from two models.
     */
    @Column(name = "fingerprint")
    public String fingerprint;

    @Column(name = "parser_version", nullable = false)
    public int parserVersion = 0;

    @Column(name = "embedding_model")
    public String embeddingModel;

    /** The index generation currently serving. Zero means nothing has been activated. */
    @Column(name = "active_generation", nullable = false)
    public int activeGeneration = 0;

    @Column(name = "document_count", nullable = false)
    public int documentCount = 0;

    @Column(name = "chunk_count", nullable = false)
    public int chunkCount = 0;

    @Column(name = "harmony_count", nullable = false)
    public int harmonyCount = 0;

    @Column(name = "retrieved_at")
    public Instant retrievedAt;

    @Column(name = "last_error", length = 4000)
    public String lastError;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    public boolean isRetrievable() {
        return enabled && state.isRetrievable();
    }

    public static KnowledgeSource byId(String id) {
        return findById(id);
    }

    public static List<KnowledgeSource> retrievable() {
        return list("enabled = true and state = ?1", SourceState.ACTIVE);
    }
}
