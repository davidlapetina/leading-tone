package fr.lapetina.music.knowledge.license;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.source.SourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The licence rules, which exist in code rather than in a README because a rule that lives
 * only in documentation is one that gets forgotten during the next refactor.
 */
class LicensePolicyServiceTest {

    private LicensePolicyService policy;

    @BeforeEach
    void setUp() {
        SourceRegistry registry = new SourceRegistry();
        registry.load();
        policy = new LicensePolicyService(registry);
    }

    @Test
    @DisplayName("material whose licence nobody established is never ingested")
    void refusesUnknownLicences() {
        assertFalse(policy.canIngest(SourceLicense.unknown()));
        assertFalse(policy.isLicenseKnown(SourceLicense.unknown()));
        assertFalse(policy.canIngestUrl("https://example.org/some-bespoke-terms"));
        assertFalse(policy.canIngestUrl(null));
        assertFalse(policy.canIngestUrl(""));
    }

    @Test
    @DisplayName("All Rights Reserved means exactly that")
    void refusesReservedMaterial() {
        assertFalse(policy.canIngestUrl("https://choosealicense.com/no-license/"));
        assertEquals(LicenseStatus.REJECTED, policy.resolve("https://choosealicense.com/no-license/").status());
    }

    @Test
    void acceptsTheCreativeCommonsLicencesWeUse() {
        assertTrue(policy.canIngestUrl("https://creativecommons.org/licenses/by-sa/4.0/"));
        assertTrue(policy.canIngestUrl("https://creativecommons.org/licenses/by-nc-sa/4.0/"));
    }

    @Test
    @DisplayName("publishers write the same licence a dozen ways, and all of them resolve")
    void normalisesLicenceUrls() {
        assertEquals("CC-BY-SA-4.0", policy.resolve("https://creativecommons.org/licenses/by-sa/4.0/").id());
        assertEquals("CC-BY-SA-4.0", policy.resolve("http://creativecommons.org/licenses/by-sa/4.0").id());
        assertEquals("CC-BY-SA-4.0", policy.resolve("https://www.creativecommons.org/licenses/by-sa/4.0/deed.en").id());
        assertEquals("CC-BY-SA-4.0", policy.resolve("https://creativecommons.org/licenses/by-sa/4.0/legalcode").id());
    }

    @Test
    @DisplayName("an unrecognised licence URL is unknown, never assumed to be permissive")
    void neverGuesses() {
        assertTrue(LicenseUrls.identify("https://creativecommons.org/licenses/by-sa/5.0/").isEmpty());
        assertEquals(SourceLicense.unknown(), policy.resolve("https://creativecommons.org/licenses/by-sa/5.0/"));
    }

    @Test
    void surfacesTheObligationsThatSurviveIngestion() {
        SourceLicense shareAlike = policy.resolve("https://creativecommons.org/licenses/by-sa/4.0/");
        assertTrue(policy.requiresAttribution(shareAlike));
        assertTrue(policy.requiresShareAlike(shareAlike));
        assertTrue(policy.allowsCommercialUse(shareAlike));

        SourceLicense nonCommercial = policy.resolve("https://creativecommons.org/licenses/by-nc-sa/4.0/");
        assertTrue(policy.requiresAttribution(nonCommercial));
        assertTrue(policy.requiresShareAlike(nonCommercial));
        assertFalse(policy.allowsCommercialUse(nonCommercial),
                "embedding and indexing do not remove a NonCommercial restriction");
    }

    @Test
    void refusingToIngestSaysWhy() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> policy.requireIngestable("mystery-source", SourceLicense.unknown()));
        assertTrue(failure.getMessage().contains("mystery-source"));
        assertTrue(failure.getMessage().contains("unknown"));
    }
}
