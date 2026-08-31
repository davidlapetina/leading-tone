package fr.lapetina.music.knowledge.embedding;

/**
 * Which model produced the vectors in an index.
 *
 * <p>Recorded with the index so that a model change is detected rather than discovered.
 * Vectors from two models are not comparable, and an index quietly holding both would
 * return confidently wrong neighbours.
 */
public record EmbeddingModelInfo(String name, String version, int dimension) {

    public static final EmbeddingModelInfo NONE = new EmbeddingModelInfo("none", "0", 0);

    public boolean isPresent() {
        return dimension > 0;
    }

    /** Two indexes may only share vectors when this matches exactly. */
    public String signature() {
        return name + "/" + version + "/" + dimension;
    }
}
