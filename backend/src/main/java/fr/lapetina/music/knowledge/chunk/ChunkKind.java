package fr.lapetina.music.knowledge.chunk;

/** What sort of passage a chunk is, which affects how it is ranked. */
public enum ChunkKind {
    /** Ordinary explanatory text. */
    PROSE,
    /** A definition, usually the opening of a section. */
    DEFINITION,
    /** A worked example kept attached to the text that introduces it. */
    EXAMPLE,
    /** An Open Music Theory "Key Takeaways" block: the densest text on the page. */
    TAKEAWAY
}
