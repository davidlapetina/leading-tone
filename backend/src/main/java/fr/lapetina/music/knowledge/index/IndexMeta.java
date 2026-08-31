package fr.lapetina.music.knowledge.index;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.lapetina.music.knowledge.embedding.EmbeddingModelInfo;
import java.time.Instant;

/**
 * What an index generation is, written beside it so it can be read without opening it.
 *
 * <p>The embedding signature is the important field. If it does not match the embedder
 * configured now, the generation is opened for lexical search only and the source is
 * marked as needing a rebuild, rather than serving neighbours computed by a model that is
 * no longer in use.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndexMeta(
        int generation,
        String embeddingModel,
        String embeddingVersion,
        int embeddingDimension,
        int analyzerVersion,
        int chunkPolicyVersion,
        String luceneVersion,
        Instant createdAt,
        long documentCount,
        long chunkCount) {

    public EmbeddingModelInfo embedding() {
        return new EmbeddingModelInfo(embeddingModel, embeddingVersion, embeddingDimension);
    }

    public boolean hasVectors() {
        return embeddingDimension > 0;
    }

    /** Whether vectors in this generation may be compared with vectors from this embedder. */
    public boolean matches(EmbeddingModelInfo current) {
        return hasVectors() && embedding().signature().equals(current.signature());
    }
}
