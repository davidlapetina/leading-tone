package fr.lapetina.music.knowledge.source;

/** How a source is meant to be used once ingested. */
public enum IngestionMode {

    /** Prose, chunked and embedded, retrieved to ground an explanation. */
    TEXT_RAG,

    /**
     * Harmonic annotations of real music, parsed into structured records and queried
     * by composer, key and Roman numeral. Deliberately not flattened into vectors:
     * "find a Beethoven example of V/V" is a query, not a similarity search.
     */
    STRUCTURED_HARMONY
}
