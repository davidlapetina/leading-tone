package fr.lapetina.music.knowledge.embedding;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The embedding model, running in process on ONNX Runtime.
 *
 * <p>bge-small-en-v1.5, quantized, ships inside the jar: 384 dimensions, no download at
 * first use, no HTTP call, no separate service to keep alive. This is the main reason the
 * distributed jar is large, and the reason the application still works with no network.
 *
 * <p>Loading is deferred until the first embedding is actually needed, so a deployment
 * that never ingests anything never pays for it.
 */
@ApplicationScoped
public class OnnxEmbeddingService implements EmbeddingService {

    private static final Logger LOG = Logger.getLogger(OnnxEmbeddingService.class);

    /** The dimension is observed from the model, never configured: a number you can set is a number you can set wrong. */
    private static final String MODEL_NAME = "bge-small-en-v1.5-q";
    private static final String MODEL_VERSION = "1.5";

    @ConfigProperty(name = "music.knowledge.embedding.enabled", defaultValue = "true")
    boolean enabled;

    private volatile EmbeddingModel model;
    private volatile EmbeddingModelInfo info = EmbeddingModelInfo.NONE;
    private volatile boolean failed;

    @Override
    public boolean isAvailable() {
        return enabled && !failed;
    }

    @Override
    public EmbeddingModelInfo info() {
        if (!isAvailable()) {
            return EmbeddingModelInfo.NONE;
        }
        ensureLoaded();
        return info;
    }

    @Override
    public float[] embed(String text) {
        if (!isAvailable()) {
            throw new IllegalStateException("No embedding model is available");
        }
        ensureLoaded();
        return EmbeddingService.normalise(model.embed(TextSegment.from(text)).content().vector());
    }

    private void ensureLoaded() {
        if (model != null) {
            return;
        }
        synchronized (this) {
            if (model != null) {
                return;
            }
            try {
                long started = System.currentTimeMillis();
                EmbeddingModel loaded = new BgeSmallEnV15QuantizedEmbeddingModel();
                int dimension = loaded.embed(TextSegment.from("probe")).content().vector().length;
                this.info = new EmbeddingModelInfo(MODEL_NAME, MODEL_VERSION, dimension);
                this.model = loaded;
                LOG.infof("Embedding model %s ready, %d dimensions, %d ms",
                        info.signature(), dimension, System.currentTimeMillis() - started);
            } catch (RuntimeException | LinkageError e) {
                this.failed = true;
                LOG.warnf("No embedding model: %s. Retrieval will use lexical search only.", e.toString());
                throw new IllegalStateException("Embedding model unavailable", e);
            }
        }
    }
}
