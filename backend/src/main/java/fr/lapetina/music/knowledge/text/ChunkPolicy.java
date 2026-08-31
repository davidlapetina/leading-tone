package fr.lapetina.music.knowledge.text;

/**
 * How large a passage should be.
 *
 * <p>These are targets, not rules. A definition and the example that explains it are worth
 * more together than either is at the right length, so the chunker will overshoot
 * {@code targetChars} to keep them in one piece. Conceptual integrity beats token count.
 *
 * @param version bumped whenever the chunking behaviour changes, so that a policy change
 *     forces a rebuild rather than leaving an index holding passages cut two different ways
 */
public record ChunkPolicy(int targetChars, int maxChars, int minChars, int version) {

    public static final int CURRENT_VERSION = 1;

    public static ChunkPolicy defaults() {
        return new ChunkPolicy(1200, 1800, 250, CURRENT_VERSION);
    }
}
