package fr.lapetina.music.knowledge.ingestion;

import fr.lapetina.music.knowledge.harmony.HarmonyEvent;
import fr.lapetina.music.knowledge.source.SourceManifest;
import java.util.List;

/**
 * Reads a corpus of harmonic annotations.
 *
 * <p>Separate from {@link Ingester} because the output is different in kind. Explanatory
 * text becomes passages to quote; a corpus becomes rows to query. Flattening real musical
 * analysis into prose chunks would throw away the thing that makes it worth having — that
 * you can ask it for a Beethoven example of V/V and get an actual measure number.
 */
public interface HarmonyIngester {

    String sourceId();

    int parserVersion();

    Harvest harvest(SourceManifest.ManifestSource source, int generation);

    record Harvest(List<HarmonyEvent> events, int worksSeen, int filesSeen, List<String> problems) {
        public Harvest {
            events = events == null ? List.of() : List.copyOf(events);
            problems = problems == null ? List.of() : List.copyOf(problems);
        }
    }
}
