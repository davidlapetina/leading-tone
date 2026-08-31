package fr.lapetina.music.knowledge.harmony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.source.IngestionMode;
import fr.lapetina.music.knowledge.source.KnowledgeSource;
import fr.lapetina.music.knowledge.source.SourceState;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A progression is a relationship between chords, not a label on one of them.
 *
 * <p>Answering "show me a ii-V-I" by finding a chord labelled {@code ii7} would be a
 * different claim from the one being taught, and would look right often enough to be
 * trusted. So consecutive runs are matched instead.
 */
@QuarkusTest
class ProgressionSearchTest {

    private static final String SOURCE = "dcml-mozart-piano-sonatas";

    @Inject
    HarmonySearchService harmonySearch;

    @BeforeEach
    void writeAScoreWithOneRealTwoFiveOne() {
        activate();
        writeEvents();
    }

    @Transactional
    void activate() {
        KnowledgeSource source = KnowledgeSource.byId(SOURCE);
        if (source == null) {
            source = new KnowledgeSource();
            source.id = SOURCE;
            source.displayName = "Mozart";
            source.ingestionMode = IngestionMode.STRUCTURED_HARMONY;
        }
        source.licenseId = "CC-BY-NC-SA-4.0";
        source.state = SourceState.ACTIVE;
        source.enabled = true;
        source.updatedAt = Instant.now();
        source.persist();
    }

    @Transactional
    void writeEvents() {
        HarmonyEvent.deleteAll();
        // A genuine ii-V-I in bars 1-3, and a ii that goes somewhere else in bars 10-12.
        int measure = 1;
        for (String numeral : List.of("ii", "V", "I")) {
            write(numeral, measure++);
        }
        measure = 10;
        for (String numeral : List.of("ii", "vi", "IV")) {
            write(numeral, measure++);
        }
    }

    private void write(String numeral, int measure) {
        HarmonyEvent event = new HarmonyEvent();
        event.sourceId = SOURCE;
        event.generation = 1;
        event.composer = "Wolfgang Amadeus Mozart";
        event.work = "Sonata no. 1";
        event.measure = measure;
        event.beat = 0.0;
        event.globalKey = "C major";
        event.romanNumeral = numeral;
        event.sourceReference = "harmonies/K279-1.harmonies.tsv, m. " + measure;
        event.licenseId = "CC-BY-NC-SA-4.0";
        event.active = true;
        event.persist();
    }

    @Test
    @DisplayName("a real run is found, and cited from where it begins")
    void findsAConsecutiveRun() {
        List<MusicalExample> found = harmonySearch.findProgressions(List.of("ii", "V", "I"), null, null, 5);

        assertEquals(1, found.size(), "there is one ii-V-I in this score, not two");
        assertEquals(1, found.get(0).measure(), "cited from the bar the progression starts in");
        assertEquals("ii", found.get(0).romanNumeral());
    }

    @Test
    @DisplayName("a chord that merely starts like the pattern is not a match")
    void doesNotMatchAChordThatGoesElsewhere() {
        assertTrue(harmonySearch.findProgressions(List.of("ii", "V", "vii"), null, null, 5).isEmpty(),
                "no run of ii-V-vii exists here, and the lone ii must not stand in for one");
        assertTrue(harmonySearch.findProgressions(List.of("ii", "vi", "I"), null, null, 5).isEmpty());
    }

    @Test
    void findsTheOtherRunToo() {
        assertEquals(10, harmonySearch.findProgressions(List.of("ii", "vi", "IV"), null, null, 5)
                .get(0).measure());
    }

    @Test
    void refusesAPatternTooShortToBeAProgression() {
        assertTrue(harmonySearch.findProgressions(List.of("V"), null, null, 5).isEmpty());
        assertTrue(harmonySearch.findProgressions(List.of(), null, null, 5).isEmpty());
        assertTrue(harmonySearch.findProgressions(null, null, null, 5).isEmpty());
    }

    @Test
    @DisplayName("the corpus filter is honoured, so a jazz concept is not shown a sonata")
    void honoursTheCorpusFilter() {
        assertFalse(harmonySearch.findProgressions(List.of("ii", "V", "I"), null, "dcml-", 5).isEmpty());
        assertTrue(harmonySearch.findProgressions(List.of("ii", "V", "I"), null, "jazz-", 5).isEmpty());
    }

    @Test
    void everyResultCarriesTheRowItCameFrom() {
        MusicalExample example = harmonySearch.findProgressions(List.of("ii", "V", "I"), null, null, 5).get(0);

        assertTrue(example.eventId() != null, "a citation must be traceable to its annotation");
        assertEquals(ExampleOrigin.VERIFIED_CORPUS, example.origin());
        assertEquals("CC-BY-NC-SA-4.0", example.licenseId());
    }
}
