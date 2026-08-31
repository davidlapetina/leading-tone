package fr.lapetina.music.knowledge.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.index.KnowledgePaths;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The local copy of downloaded sources.
 *
 * <p>The interesting property is where files land. A remote resource must not be able to
 * influence that, so names come from a hash of the URL rather than from the URL text.
 */
class RawStoreTest {

    private RawStore store;
    private KnowledgePaths paths;

    @BeforeEach
    void setUp(@TempDir Path directory) {
        paths = new KnowledgePaths(directory);
        store = new RawStore();
        store.paths = paths;
    }

    @Test
    void keepsWhatItWasGiven() {
        store.write("open-music-theory", "https://example.org/a", "the body");

        assertEquals("the body", store.read("open-music-theory", "https://example.org/a").orElseThrow());
        assertTrue(store.read("open-music-theory", "https://example.org/b").isEmpty());
    }

    @Test
    @DisplayName("two sources do not share a cache entry even for the same URL")
    void keepsSourcesApart() {
        store.write("a", "https://example.org/x", "from a");
        store.write("b", "https://example.org/x", "from b");

        assertEquals("from a", store.read("a", "https://example.org/x").orElseThrow());
        assertEquals("from b", store.read("b", "https://example.org/x").orElseThrow());
    }

    @Test
    @DisplayName("a URL cannot choose where on this disk it is written")
    void refusesPathTraversal() {
        Path root = paths.rawFor("open-music-theory").normalize();

        for (String hostile : new String[]{
                "https://example.org/../../../../etc/passwd",
                "https://example.org/a/../../b",
                "https://example.org/..%2f..%2fetc",
                "https://example.org/"}) {
            Path resolved = store.fileFor("open-music-theory", hostile).normalize();
            assertTrue(resolved.startsWith(root), hostile + " escaped to " + resolved);
        }
    }

    @Test
    @DisplayName("a source id cannot escape either")
    void sanitisesTheSourceId() {
        Path resolved = store.fileFor("../../evil", "https://example.org/a").normalize();

        assertTrue(resolved.startsWith(paths.root().normalize()), resolved.toString());
        assertFalse(resolved.toString().contains(".."));
    }

    @Test
    void forgettingASourceRemovesItsCopies() {
        store.write("open-music-theory", "https://example.org/a", "body");
        assertTrue(store.sizeOf("open-music-theory") > 0);

        store.forget("open-music-theory");

        assertTrue(store.read("open-music-theory", "https://example.org/a").isEmpty());
        assertEquals(0, store.sizeOf("open-music-theory"));
    }
}
