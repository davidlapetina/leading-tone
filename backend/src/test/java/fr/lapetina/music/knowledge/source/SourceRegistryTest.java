package fr.lapetina.music.knowledge.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.license.SourceLicense;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates the manifest that actually ships, so a bad edit fails here rather than
 * producing a wrong attribution in front of a learner.
 */
class SourceRegistryTest {

    private SourceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SourceRegistry();
        registry.load();
    }

    @Test
    void loadsEverySourceWeDeclare() {
        assertEquals(14, registry.all().size(),
                "Open Music Theory, twelve DCML corpora and the Jazz Harmony Treebank");
        assertTrue(registry.isDeclared("open-music-theory"));
        assertTrue(registry.isDeclared("jazz-harmony-treebank"));
        assertEquals(12, registry.all().stream().filter(s -> s.id().startsWith("dcml-")).count());
    }

    @Test
    @DisplayName("every source names a licence that is actually in the vocabulary")
    void everySourceHasAKnownLicence() {
        for (SourceManifest.ManifestSource source : registry.all()) {
            SourceLicense license = registry.licenseOf(source);
            assertNotNull(license, source.id());
            assertNotEqualsUnknown(source.id(), license);
            assertNotNull(license.url(), source.id() + " has no licence URL");
        }
    }

    private static void assertNotEqualsUnknown(String id, SourceLicense license) {
        assertFalse("UNKNOWN".equals(license.id()), id + " resolves to an unknown licence");
    }

    @Test
    @DisplayName("every source we would credit carries the citation its publisher asks for")
    void everySourceCarriesItsCitation() {
        for (SourceManifest.ManifestSource source : registry.all()) {
            assertNotNull(source.citation(), source.id() + " has no citation");
            assertFalse(source.citation().isBlank(), source.id() + " has a blank citation");
        }
    }

    @Test
    @DisplayName("the corpora are NonCommercial, and the manifest says so")
    void recordsTheNonCommercialRestriction() {
        List<String> nonCommercial = registry.all().stream()
                .filter(source -> !registry.licenseOf(source).commercialUseAllowed())
                .map(SourceManifest.ManifestSource::id)
                .toList();

        assertTrue(nonCommercial.contains("jazz-harmony-treebank"));
        assertTrue(nonCommercial.contains("dcml-mozart-piano-sonatas"));
        assertEquals(13, nonCommercial.size(), "every corpus is CC BY-NC-SA; only the textbook is not");
        assertTrue(registry.licenseOf(registry.require("open-music-theory")).commercialUseAllowed());
    }

    @Test
    void refusesASourceNobodyDeclared() {
        assertFalse(registry.isDeclared("some-website-someone-pasted"));
        assertThrows(IllegalArgumentException.class, () -> registry.require("some-website-someone-pasted"));
    }

    @Test
    @DisplayName("a manifest naming a licence that does not exist stops the application")
    void rejectsAnUndeclaredLicence() {
        SourceManifest broken = new SourceManifest(
                List.of(new SourceManifest.ManifestSource("x", "X", null, null, "https://example.org",
                        null, null, null, null, null, null, null, null, "cite",
                        "NO-SUCH-LICENCE", IngestionMode.TEXT_RAG, Tradition.GENERAL, true, null)),
                List.of());

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> registry.index(broken));
        assertTrue(failure.getMessage().contains("NO-SUCH-LICENCE"), failure.getMessage());
    }
}
