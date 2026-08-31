package fr.lapetina.music.knowledge.ingestion.omt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.ingestion.DocumentDraft;
import fr.lapetina.music.knowledge.ingestion.Ingester;
import fr.lapetina.music.knowledge.license.LicensePolicyService;
import fr.lapetina.music.knowledge.source.SourceManifest;
import fr.lapetina.music.knowledge.source.SourceRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Open Music Theory is CC BY-SA 4.0 as a book, but not as a collection of chapters: of its
 * 140 chapters one is CC BY-NC-SA 4.0 and one is All Rights Reserved. These tests pin the
 * behaviour that follows from that, against recorded copies of the real API responses.
 */
class OpenMusicTheoryIngesterTest {

    private static final long ALL_RIGHTS_RESERVED_CHAPTER = 7289;   // "Composing with Twelve Tones"
    private static final long NON_COMMERCIAL_CHAPTER = 850;         // "Fragile, Absent, and Emergent Tonics"

    private FixturePressbooksClient client;
    private OpenMusicTheoryIngester ingester;
    private SourceManifest.ManifestSource source;

    @BeforeEach
    void setUp() {
        SourceRegistry registry = new SourceRegistry();
        registry.load();
        client = new FixturePressbooksClient();
        ingester = new OpenMusicTheoryIngester();
        ingester.client = client;
        ingester.licensePolicy = new LicensePolicyService(registry);
        source = registry.require("open-music-theory");
    }

    @Test
    @DisplayName("the All Rights Reserved chapter is never even requested")
    void neverDownloadsAllRightsReservedMaterial() {
        Ingester.Harvest harvest = ingester.harvest(source);

        assertFalse(client.requestedChapters().contains(ALL_RIGHTS_RESERVED_CHAPTER),
                "the reserved chapter was fetched: " + client.requestedChapters());
        assertTrue(harvest.documents().stream().noneMatch(d -> d.title().contains("Twelve Tones")));
        assertTrue(harvest.skippedForLicense().stream().anyMatch(s -> s.contains("Twelve Tones")),
                "the refusal should be reported, not silent: " + harvest.skippedForLicense());
    }

    @Test
    @DisplayName("a chapter under a different licence is still ingested, under its own terms")
    void keepsTheNonCommercialChapterUnderItsOwnLicence() {
        Ingester.Harvest harvest = ingester.harvest(source);

        DocumentDraft nonCommercial = harvest.documents().stream()
                .filter(d -> d.externalId().equals(Long.toString(NON_COMMERCIAL_CHAPTER)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the CC BY-NC-SA chapter should be ingested"));

        assertEquals("CC-BY-NC-SA-4.0", nonCommercial.licenseId(),
                "it must not inherit the book's CC BY-SA licence");
    }

    @Test
    void ingestsTheOrdinaryChaptersUnderTheBookLicence() {
        Ingester.Harvest harvest = ingester.harvest(source);

        List<DocumentDraft> shareAlike = harvest.documents().stream()
                .filter(d -> d.licenseId().equals("CC-BY-SA-4.0"))
                .toList();
        assertEquals(4, shareAlike.size());
        assertTrue(shareAlike.stream().anyMatch(d -> d.title().contains("V–I")));
    }

    @Test
    @DisplayName("a chapter's own author is credited, not the book's author list")
    void preservesChapterAuthorship() {
        DocumentDraft twoFiveOne = ingester.harvest(source).documents().stream()
                .filter(d -> d.externalId().equals("2629"))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of("Megan Lavengood"), twoFiveOne.authors());
        assertTrue(twoFiveOne.attribution().contains("Megan Lavengood"), twoFiveOne.attribution());
        assertTrue(twoFiveOne.attribution().contains("CC BY-SA 4.0"), twoFiveOne.attribution());
        assertTrue(twoFiveOne.attribution().contains("Erin K. Maher"), twoFiveOne.attribution());
    }

    @Test
    void reportsWhatItSawAndWhatItRefused() {
        Ingester.Harvest harvest = ingester.harvest(source);

        assertEquals(6, harvest.seen());
        assertEquals(5, harvest.documents().size());
        assertEquals(1, harvest.skippedForLicense().size());
    }
}
