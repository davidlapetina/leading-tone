package fr.lapetina.music.knowledge.ingestion;

import fr.lapetina.music.knowledge.index.KnowledgePaths;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

/**
 * Keeps a local copy of everything downloaded, so a source is fetched once.
 *
 * <p>Re-ingesting then costs nothing and needs no network: changing the chunk policy or the
 * embedding model rebuilds from what is already on disk instead of asking the publisher for
 * the same 140 chapters again. It is also the polite thing to do to a server we do not own.
 *
 * <p>Lives under the gitignored data directory. It is a cache of somebody else's work, not
 * part of this repository, and it keeps the licence of the source it came from.
 *
 * <p>File names are derived from a hash of the URL, never from the URL text. A remote
 * resource must not be able to choose where on this disk it lands.
 */
@ApplicationScoped
public class RawStore {

    private static final Logger LOG = Logger.getLogger(RawStore.class);

    @Inject
    KnowledgePaths paths;

    public Optional<String> read(String sourceId, String url) {
        Path file = fileFor(sourceId, url);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException unreadable) {
            LOG.debugf("Ignoring unreadable cache entry %s: %s", file, unreadable.toString());
            return Optional.empty();
        }
    }

    public void write(String sourceId, String url, String body) {
        Path file = fileFor(sourceId, url);
        try {
            paths.ensure(file.getParent());
            Files.writeString(file, body);
        } catch (IOException e) {
            // A cache that cannot be written is a slow application, not a broken one.
            LOG.debugf("Could not cache %s: %s", url, e.toString());
        }
    }

    /** Drops one source's copies, so the next ingestion goes back to the publisher. */
    public void forget(String sourceId) {
        KnowledgePaths.deleteRecursively(paths.rawFor(sourceId));
    }

    public long sizeOf(String sourceId) {
        Path directory = paths.rawFor(sourceId);
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).mapToLong(RawStore::lengthOf).sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long lengthOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException unreadable) {
            return 0;
        }
    }

    /**
     * A name derived from the URL's hash, with a readable hint attached.
     *
     * <p>The hint is sanitised to letters, digits, dot and dash. Using the URL path directly
     * would let a resource containing {@code ../} decide where it is written, which is the
     * whole of the path-traversal problem in one line.
     */
    Path fileFor(String sourceId, String url) {
        String hash = Checksums.of(url).substring(0, 16);
        String hint = url.substring(url.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9.-]", "_");
        if (hint.length() > 60) {
            hint = hint.substring(0, 60);
        }
        String name = hint.isBlank() ? hash : hash + "-" + hint;
        Path directory = paths.rawFor(sanitise(sourceId));
        Path file = directory.resolve(name).normalize();
        if (!file.startsWith(directory.normalize())) {
            throw new IllegalArgumentException("Refusing to write outside the source directory");
        }
        return file;
    }

    /**
     * Source ids are kebab-case, so dropping everything else loses nothing real. Dots are
     * excluded deliberately: a directory named with them is safe, but a name with no dots
     * at all leaves nothing to reason about.
     */
    private static String sanitise(String sourceId) {
        String safe = sourceId.replaceAll("[^A-Za-z0-9-]", "_");
        return safe.isBlank() ? "unnamed" : safe;
    }
}
