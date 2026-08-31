package fr.lapetina.music.knowledge.ingestion.jht;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.knowledge.harmony.HarmonyEvent;
import fr.lapetina.music.knowledge.ingestion.HarmonyIngester;
import fr.lapetina.music.knowledge.ingestion.SourceFetcher;
import fr.lapetina.music.knowledge.source.SourceManifest;
import fr.lapetina.music.theory.ChordAnalysis;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.ProgressionAnalyzer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Reads the Jazz Harmony Treebank.
 *
 * <p>Each chord is also given a Roman numeral, worked out by this application's own theory
 * engine against the tune's key. The corpus publishes chord symbols, and a chord symbol
 * alone cannot answer "find me a minor two-five-one" — the function can.
 *
 * <p>Note what the corpus actually contains, which the brief did not say: of its 1170
 * tunes, all carry chords, measures and beats, but only 150 carry constituent trees and
 * 241 a turnaround. So hierarchical structure is available for part of the corpus, and the
 * ingester must not assume otherwise.
 */
@ApplicationScoped
public class JazzTreebankIngester implements HarmonyIngester {

    private static final Logger LOG = Logger.getLogger(JazzTreebankIngester.class);
    private static final int PARSER_VERSION = 1;

    @Inject
    SourceFetcher fetcher;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public String sourceId() {
        return "jazz-harmony-treebank";
    }

    @Override
    public int parserVersion() {
        return PARSER_VERSION;
    }

    @Override
    public Harvest harvest(SourceManifest.ManifestSource source, int generation) {
        String url = rawUrl(source);
        List<HarmonyEvent> events = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        int tunes = 0;
        int withTrees = 0;

        try {
            JsonNode treebank = objectMapper.readTree(fetcher.get(source.id(), url));
            for (JsonNode tune : treebank) {
                try {
                    boolean hasTrees = tune.has("trees") && !tune.path("trees").isEmpty();
                    if (hasTrees) {
                        withTrees++;
                    }
                    events.addAll(readTune(tune, source, generation, hasTrees));
                    tunes++;
                } catch (RuntimeException e) {
                    problems.add(tune.path("title").asText("?") + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the treebank: " + e.getMessage(), e);
        }
        LOG.infof("Jazz Harmony Treebank: %d tunes, %d with constituent trees, %d chord events",
                tunes, withTrees, events.size());
        return new Harvest(events, tunes, 1, problems);
    }

    List<HarmonyEvent> readTune(JsonNode tune, SourceManifest.ManifestSource source,
                                int generation, boolean hasTrees) {
        String title = tune.path("title").asText(null);
        String keyName = JazzChordSymbols.toKeyName(tune.path("key").asText(null));
        Optional<Key> key = keyName == null ? Optional.empty() : Key.tryParse(keyName);

        JsonNode chords = tune.path("chords");
        JsonNode measures = tune.path("measures");
        JsonNode beats = tune.path("beats");

        List<HarmonyEvent> events = new ArrayList<>();
        for (int i = 0; i < chords.size(); i++) {
            String published = chords.get(i).asText(null);
            String leadSheet = JazzChordSymbols.toLeadSheet(published);
            if (leadSheet == null) {
                continue;
            }
            HarmonyEvent event = new HarmonyEvent();
            event.sourceId = source.id();
            event.generation = generation;
            event.composer = tune.path("composers").asText("Unknown");
            event.work = title;
            event.movement = hasTrees ? "with constituent tree" : null;
            event.measure = i < measures.size() ? measures.get(i).asInt() : null;
            event.beat = i < beats.size() ? beats.get(i).asDouble() : null;
            event.globalKey = keyName;
            event.localKey = null;
            event.chordLabel = published;
            event.romanNumeral = key.map(context -> romanNumeralOf(leadSheet, context)).orElse(null);
            event.chordType = leadSheet;
            event.cadence = null;
            event.phraseEnd = false;
            event.sourceReference = "treebank.json: " + title
                    + (event.measure == null ? "" : ", m. " + event.measure);
            event.licenseId = source.license();
            events.add(event);
        }
        return events;
    }

    /**
     * The function of a chord in the tune's key, computed here rather than published. Null
     * when this application cannot read the chord or explain it — a blank is honest, an
     * invented numeral is not.
     */
    private String romanNumeralOf(String leadSheet, Key key) {
        try {
            ChordAnalysis analysis = ProgressionAnalyzer.analyzeChord(ChordAnalyzer.parse(leadSheet), key);
            return analysis.romanNumeral() == null ? null : analysis.romanNumeral().symbol();
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    private static String rawUrl(SourceManifest.ManifestSource source) {
        String repository = source.repository();
        String slug = repository.substring(repository.indexOf("github.com/") + "github.com/".length())
                .replaceAll("/+$", "");
        String branch = source.branch() == null ? "master" : source.branch();
        String file = source.dataFile() == null ? "treebank.json" : source.dataFile();
        return "https://raw.githubusercontent.com/" + slug + "/" + branch + "/" + file;
    }
}
