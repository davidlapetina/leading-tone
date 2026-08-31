package fr.lapetina.music.knowledge.ingestion;

import java.util.List;

/**
 * A document an ingester has read and judged fit to keep, before it is chunked and stored.
 *
 * <p>The licence is carried per document rather than taken from the source, because a
 * source's licence is only a default. Nothing reaches this record without a licence that
 * was checked.
 */
public record DocumentDraft(
        String externalId,
        String title,
        String partTitle,
        String url,
        List<String> authors,
        String licenseId,
        String attribution,
        String html) {

    public DocumentDraft {
        authors = authors == null ? List.of() : List.copyOf(authors);
    }
}
