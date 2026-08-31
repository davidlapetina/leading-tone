package fr.lapetina.music.knowledge.source;

/**
 * Where a source has got to in the ingestion pipeline.
 *
 * <p>The states are ordered, and only {@link #ACTIVE} material may be retrieved. A source
 * that fails halfway through does not become partially searchable: it stays where it
 * stopped, and the previously active index keeps serving.
 */
public enum SourceState {
    DISCOVERED,
    LICENSE_VERIFIED,
    DOWNLOADED,
    PARSED,
    INDEXED,
    ACTIVE,
    FAILED,
    DISABLED;

    public boolean isRetrievable() {
        return this == ACTIVE;
    }
}
