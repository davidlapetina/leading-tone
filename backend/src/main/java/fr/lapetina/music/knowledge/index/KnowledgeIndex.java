package fr.lapetina.music.knowledge.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.knowledge.embedding.EmbeddingModelInfo;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.jboss.logging.Logger;

/**
 * The searchable side of the knowledge layer: one open index generation, and a safe way to
 * swap it for another.
 *
 * <p>Two decisions worth knowing about.
 *
 * <p><strong>This holds no {@code IndexWriter}.</strong> Writing happens in
 * {@link IndexBuilder}, on a directory nothing is reading, for the duration of one
 * ingestion. A long-lived writer leaves a {@code write.lock} behind after a crash and
 * blocks the next start, and it makes development-mode reload fail on every save. Since a
 * generation is written once and never appended to, separating them costs nothing.
 *
 * <p><strong>A generation is immutable.</strong> Activating a new one opens it first,
 * publishes it, and only then closes the old one, so a search already in flight finishes
 * against the index it started on.
 */
@ApplicationScoped
public class KnowledgeIndex {

    private static final Logger LOG = Logger.getLogger(KnowledgeIndex.class);

    @Inject
    KnowledgePaths paths;

    @Inject
    ObjectMapper objectMapper;

    private volatile Open open;
    private volatile String unavailableReason;

    private record Open(int generation, Directory directory, SearcherManager searchers, IndexMeta meta) {}

    /** What a caller does with a searcher. */
    @FunctionalInterface
    public interface SearchTask<T> {
        T run(IndexSearcher searcher) throws IOException;
    }

    public boolean isOpen() {
        return open != null;
    }

    public Optional<IndexMeta> meta() {
        Open current = open;
        return current == null ? Optional.empty() : Optional.of(current.meta());
    }

    public int activeGeneration() {
        Open current = open;
        return current == null ? 0 : current.generation();
    }

    /** Whether this generation's vectors were made by the embedder in use now. */
    public boolean isVectorCapable(EmbeddingModelInfo current) {
        Open live = open;
        return live != null && live.meta().matches(current);
    }

    public Optional<String> unavailableReason() {
        return Optional.ofNullable(unavailableReason);
    }

    /** Records that the index cannot be used, so retrieval degrades instead of throwing. */
    public void unavailable(String reason) {
        this.unavailableReason = reason;
        LOG.warnf("Knowledge index unavailable: %s", reason);
    }

    /** Opens whatever generation the disk says is current. Safe to call when there is none. */
    public synchronized void openCurrent() {
        int generation = paths.readCurrent();
        if (generation <= 0) {
            return;
        }
        try {
            activate(generation);
        } catch (RuntimeException e) {
            unavailable("could not open index generation " + generation + ": " + e.getMessage());
        }
    }

    /**
     * Swaps in a generation. The new one is opened and published before the old one is
     * closed, so no search ever sees a half-open index.
     */
    public synchronized void activate(int generation) {
        Path directory = paths.generation(generation);
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("No index generation at " + directory);
        }
        Open previous = open;
        try {
            Directory opened = MMapDirectory.open(directory);
            SearcherManager searchers = new SearcherManager(opened, null);
            IndexMeta meta = readMeta(directory, generation);
            this.open = new Open(generation, opened, searchers, meta);
            this.unavailableReason = null;
            LOG.infof("Knowledge index generation %d active: %d chunks, embedding %s",
                    generation, meta.chunkCount(),
                    meta.hasVectors() ? meta.embedding().signature() : "none (lexical search only)");
        } catch (IOException e) {
            throw new IllegalStateException("Could not open index generation " + generation, e);
        }
        closeQuietly(previous);
    }

    private IndexMeta readMeta(Path directory, int generation) throws IOException {
        Path file = directory.resolve("index-meta.json");
        if (!Files.exists(file)) {
            return new IndexMeta(generation, "none", "0", 0, MusicAnalyzer.ANALYZER_VERSION,
                    0, "unknown", java.time.Instant.now(), 0, 0);
        }
        return objectMapper.readValue(Files.readString(file), IndexMeta.class);
    }

    /** Runs a search against the live generation, or returns the fallback when there is none. */
    public <T> T search(SearchTask<T> task, T whenClosed) {
        Open live = open;
        if (live == null) {
            return whenClosed;
        }
        IndexSearcher searcher = null;
        try {
            searcher = live.searchers().acquire();
            return task.run(searcher);
        } catch (IOException e) {
            LOG.warnf("Knowledge search failed: %s", e.toString());
            return whenClosed;
        } finally {
            if (searcher != null) {
                try {
                    live.searchers().release(searcher);
                } catch (IOException ignored) {
                    // Releasing a searcher cannot fail in a way the caller can act on.
                }
            }
        }
    }

    @PreDestroy
    void shutdown() {
        closeQuietly(open);
        open = null;
    }

    private static void closeQuietly(Open target) {
        if (target == null) {
            return;
        }
        try {
            target.searchers().close();
            target.directory().close();
        } catch (IOException e) {
            LOG.debugf("Closing index generation %d: %s", target.generation(), e.toString());
        }
    }
}
