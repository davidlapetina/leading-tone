package fr.lapetina.music.knowledge.license;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.harmony.HarmonySearchService;
import fr.lapetina.music.knowledge.harmony.HarmonyEvent;
import fr.lapetina.music.knowledge.harmony.NoteEvent;
import fr.lapetina.music.knowledge.harmony.ScoreSource;
import fr.lapetina.music.knowledge.source.IngestionMode;
import fr.lapetina.music.knowledge.source.KnowledgeSource;
import fr.lapetina.music.knowledge.source.SourceState;
import fr.lapetina.music.settings.SettingsService;
import fr.lapetina.music.settings.SettingsUpdate;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Jazz Harmony Treebank is CC BY-NC-SA 4.0, and so is every annotated score corpus.
 *
 * <p>A NonCommercial condition is not discharged by downloading, parsing, embedding or
 * indexing the material, so a commercial deployment must not serve it — and must not serve
 * it by <em>any</em> route. This test exists because an enforcement rule that holds on the
 * route somebody remembered to test, and not on the others, is worse than none: it reads
 * as protection.
 */
@QuarkusTest
class CommercialModeTest {

    private static final String JAZZ = "jazz-harmony-treebank";
    private static final String TEXTBOOK = "open-music-theory";

    @Inject
    LicensePolicyService policy;

    @Inject
    HarmonySearchService harmonySearch;

    @Inject
    ScoreSource scoreSource;

    @Inject
    SettingsService settingsService;

    @BeforeEach
    void makeBothSourcesActive() {
        settingsService.reset();
        activate(JAZZ, "CC-BY-NC-SA-4.0");
        activate(TEXTBOOK, "CC-BY-SA-4.0");
        writeOneJazzHarmonyRow();
    }

    @AfterEach
    void leaveTheSettingsAsWeFoundThem() {
        settingsService.reset();
    }

    @Transactional
    void activate(String id, String licenseId) {
        KnowledgeSource source = KnowledgeSource.byId(id);
        if (source == null) {
            source = new KnowledgeSource();
            source.id = id;
            source.displayName = id;
            source.ingestionMode = IngestionMode.STRUCTURED_HARMONY;
        }
        source.licenseId = licenseId;
        source.state = SourceState.ACTIVE;
        source.enabled = true;
        source.updatedAt = Instant.now();
        source.persist();
    }

    @Transactional
    void writeOneJazzHarmonyRow() {
        HarmonyEvent.delete("sourceId = ?1", JAZZ);
        HarmonyEvent event = new HarmonyEvent();
        event.sourceId = JAZZ;
        event.generation = 1;
        event.composer = "Kern, Jerome";
        event.work = "All The Things You Are";
        event.measure = 1;
        event.globalKey = "Ab major";
        event.romanNumeral = "V7/V";
        event.sourceReference = "treebank.json: All The Things You Are";
        event.licenseId = "CC-BY-NC-SA-4.0";
        event.active = true;
        event.persist();
    }

    private void setMode(String mode) {
        settingsService.update(SettingsUpdate.none().withRuntimeMode(mode));
    }

    @Test
    @DisplayName("non-commercial is the default, and everything is available")
    void servesEverythingWhenNotCommercial() {
        assertEquals(RuntimeMode.NON_COMMERCIAL, policy.currentMode());

        Set<String> allowed = policy.retrievableSourceIds();
        assertTrue(allowed.contains(JAZZ));
        assertTrue(allowed.contains(TEXTBOOK));
        assertFalse(harmonySearch.findExamples("V7/V", "Kern", null, 5).isEmpty(),
                "the jazz corpus should be searchable for personal study");
    }

    @Test
    @DisplayName("commercial mode blocks the NonCommercial source on every route at once")
    void blocksNonCommercialSourcesEverywhere() {
        setMode("COMMERCIAL");
        assertEquals(RuntimeMode.COMMERCIAL, policy.currentMode());

        // The one list every retrieval path consults.
        Set<String> allowed = policy.retrievableSourceIds();
        assertFalse(allowed.contains(JAZZ), "the treebank is NonCommercial");
        assertTrue(allowed.contains(TEXTBOOK), "the textbook is CC BY-SA and stays available");

        // Route 1: structured harmony search.
        assertTrue(harmonySearch.findExamples("V7/V", "Kern", null, 5).isEmpty(),
                "structured search must not return NonCommercial material");
        assertTrue(harmonySearch.findExamples(null, null, null, 50).stream()
                        .noneMatch(example -> JAZZ.equals(example.sourceId())),
                "no filter combination may leak it");

        // Route 2: cadence search, a separate query path.
        assertTrue(harmonySearch.findCadences("PAC", null, 50).stream()
                .noneMatch(example -> JAZZ.equals(example.sourceId())));

        // Route 3: notation. Rendering the score is the same disclosure as quoting the text.
        List<NoteEvent> notes = scoreSource.notesFor(JAZZ, "harmonies/x.harmonies.tsv, m. 1", 1, 2);
        assertTrue(notes.isEmpty(), "notation must not be rendered from a blocked source");

        // Route 4: asking about the source directly.
        assertFalse(policy.canRetrieve(JAZZ));
        assertThrows(IllegalStateException.class, () -> policy.requireRetrievable(JAZZ));
    }

    @Test
    @DisplayName("switching back restores it, without re-indexing anything")
    void isDecidedPerQueryRatherThanBakedIntoTheIndex() {
        setMode("COMMERCIAL");
        assertTrue(harmonySearch.findExamples("V7/V", "Kern", null, 5).isEmpty());

        setMode("NON_COMMERCIAL");
        assertFalse(harmonySearch.findExamples("V7/V", "Kern", null, 5).isEmpty(),
                "the decision is made per query, so nothing has to be rebuilt");
    }

    @Test
    @DisplayName("an unrecognised mode is refused rather than falling back to the permissive one")
    void refusesAnUnknownMode() {
        assertThrows(IllegalArgumentException.class, () -> setMode("SORT-OF-COMMERCIAL"));
        assertEquals(RuntimeMode.NON_COMMERCIAL, policy.currentMode());
    }
}
