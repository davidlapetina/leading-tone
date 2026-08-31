package fr.lapetina.music.knowledge.embedding;

import java.util.List;

/**
 * Turns text into a vector, in this JVM.
 *
 * <p>There is no embedding server and no Python. When no embedder is available the
 * retrieval pipeline keeps working on lexical search alone, which is the same degradation
 * the tutor already makes when the language model is off.
 */
public interface EmbeddingService {

    boolean isAvailable();

    EmbeddingModelInfo info();

    /** A unit-length vector, so that a dot product is a cosine similarity. */
    float[] embed(String text);

    default List<float[]> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    /** Normalising here means the index can use DOT_PRODUCT and get cosine for free. */
    static float[] normalise(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += (double) value * value;
        }
        double length = Math.sqrt(sum);
        if (length == 0.0) {
            return vector;
        }
        float[] unit = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            unit[i] = (float) (vector[i] / length);
        }
        return unit;
    }
}
