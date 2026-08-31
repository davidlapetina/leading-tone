package fr.lapetina.music.knowledge.index;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Where the knowledge layer keeps its files.
 *
 * <p>Everything lives beside the H2 database file, under one directory, so that "where is
 * my data" has one answer and backing up means copying one folder. There is no external
 * store: the vectors are inside the Lucene index, and the Lucene index is a directory.
 *
 * <pre>
 * data/
 *   leading-tone.mv.db          the H2 file: learner, settings, chunks, provenance
 *   knowledge/
 *     sources/raw/&lt;sourceId&gt;/   the bytes as downloaded, so re-parsing needs no network
 *     index/gen-000001/         a complete Lucene index, postings and vectors together
 *     index/current             one line naming the generation now serving
 * </pre>
 */
@ApplicationScoped
public class KnowledgePaths {

    @ConfigProperty(name = "music.knowledge.data-path", defaultValue = "./data/knowledge")
    String dataPath;

    public KnowledgePaths() {
        // For CDI.
    }

    /** For tests, which need a temporary directory rather than the configured one. */
    public KnowledgePaths(java.nio.file.Path root) {
        this.dataPath = root.toString();
    }

    public Path root() {
        return Path.of(dataPath);
    }

    public Path rawFor(String sourceId) {
        return root().resolve("sources").resolve("raw").resolve(sourceId);
    }

    public Path indexRoot() {
        return root().resolve("index");
    }

    public Path generation(int generation) {
        return indexRoot().resolve(String.format("gen-%06d", generation));
    }

    public Path currentMarker() {
        return indexRoot().resolve("current");
    }

    public void ensure(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create " + path, e);
        }
    }

    /** The generation recorded on disk, or zero when nothing has been activated here. */
    public int readCurrent() {
        Path marker = currentMarker();
        if (!Files.exists(marker)) {
            return 0;
        }
        try {
            return Integer.parseInt(Files.readString(marker).trim());
        } catch (IOException | NumberFormatException unreadable) {
            return 0;
        }
    }

    public void writeCurrent(int generation) {
        ensure(indexRoot());
        try {
            Files.writeString(currentMarker(), Integer.toString(generation));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not record the active index generation", e);
        }
    }

    /** Removes generations older than the one before last. One rollback is kept, not a museum. */
    public void pruneGenerationsBefore(int keepFrom) {
        if (!Files.isDirectory(indexRoot())) {
            return;
        }
        try (Stream<Path> entries = Files.list(indexRoot())) {
            entries.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("gen-"))
                    .filter(path -> generationNumber(path) < keepFrom)
                    .forEach(KnowledgePaths::deleteRecursively);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not prune old index generations", e);
        }
    }

    static int generationNumber(Path path) {
        try {
            return Integer.parseInt(path.getFileName().toString().substring(4));
        } catch (RuntimeException notAGeneration) {
            return Integer.MAX_VALUE;
        }
    }

    public static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(entry -> {
                try {
                    Files.deleteIfExists(entry);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete " + path, e);
        }
    }
}
