package fr.lapetina.music.knowledge.chunk;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;

/**
 * One retrievable passage, carrying enough provenance to be credited on its own.
 *
 * <p>Every vector in the Lucene index resolves back to one of these rows. There are no
 * anonymous embeddings: if a passage cannot say where it came from and under what licence,
 * it does not get indexed.
 */
@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @Column(name = "document_id", nullable = false)
    public UUID documentId;

    @Column(name = "source_id", nullable = false)
    public String sourceId;

    @Column(nullable = false)
    public int generation;

    /** sha256 of document, position and text: the same passage always gets the same key. */
    @Column(name = "chunk_key", nullable = false)
    public String chunkKey;

    @Column(name = "document_title", length = 1000)
    public String documentTitle;

    @Column(name = "section_title", length = 1000)
    public String sectionTitle;

    @Column(name = "section_order", nullable = false)
    public int sectionOrder;

    @Column(name = "chunk_order", nullable = false)
    public int chunkOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ChunkKind kind = ChunkKind.PROSE;

    /** Named "body" rather than "text" so it does not shadow the Lucene field of that name. */
    @Column(nullable = false, length = 100000)
    public String body;

    /** Comma separated concept ids this passage was tagged with. */
    @Column(name = "concept_ids", length = 1000)
    public String conceptIds;

    @Column(name = "license_id", nullable = false)
    public String licenseId;

    @Column(nullable = false, length = 2000)
    public String attribution;

    @Column(length = 2000)
    public String url;

    @Column(name = "word_count", nullable = false)
    public int wordCount;

    @Column(nullable = false)
    public boolean active = false;

    public static List<KnowledgeChunk> activeFor(String sourceId) {
        return list("sourceId = ?1 and active = true", sourceId);
    }

    public static long countActive() {
        return count("active = true");
    }
}
