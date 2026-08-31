package fr.lapetina.music.knowledge.chunk;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One ingested source document, with the provenance needed to credit it.
 *
 * <p>The licence is stored per document, not taken from the source, because a source's
 * own licence is only a default. Of Open Music Theory's 140 chapters, 138 are CC BY-SA
 * 4.0, one is CC BY-NC-SA 4.0, and one is All Rights Reserved and is never fetched at all.
 *
 * <p>Nothing here is MIT. The application's licence covers the code that produced this
 * row, not the text inside it.
 */
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @Column(name = "source_id", nullable = false)
    public String sourceId;

    @Column(name = "generation", nullable = false)
    public int generation;

    /** The identifier the source itself uses: a Pressbooks chapter id, a corpus file path. */
    @Column(name = "external_id", nullable = false)
    public String externalId;

    @Column(nullable = false, length = 1000)
    public String title;

    @Column(name = "part_title", length = 1000)
    public String partTitle;

    @Column(length = 2000)
    public String url;

    /** As published, comma separated. Chapter authorship is preserved, not replaced by the book's. */
    @Column(length = 2000)
    public String authors;

    @Column(name = "license_id", nullable = false)
    public String licenseId;

    @Column(name = "attribution", nullable = false, length = 2000)
    public String attribution;

    @Column(nullable = false)
    public String checksum;

    @Column(name = "word_count", nullable = false)
    public int wordCount;

    @Column(name = "body", length = 1000000)
    public String body;

    /** Only the active generation is retrievable. Activation flips this in one transaction. */
    @Column(nullable = false)
    public boolean active = false;

    @Column(name = "ingested_at", nullable = false)
    public Instant ingestedAt = Instant.now();

    public static List<KnowledgeDocument> activeFor(String sourceId) {
        return list("sourceId = ?1 and active = true", sourceId);
    }

    public static long countActive() {
        return count("active = true");
    }
}
