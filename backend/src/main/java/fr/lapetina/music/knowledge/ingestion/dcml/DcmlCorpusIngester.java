package fr.lapetina.music.knowledge.ingestion.dcml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.knowledge.harmony.HarmonyEvent;
import fr.lapetina.music.knowledge.ingestion.HarmonyIngester;
import fr.lapetina.music.knowledge.ingestion.SourceFetcher;
import fr.lapetina.music.knowledge.ingestion.TsvTable;
import fr.lapetina.music.knowledge.source.SourceManifest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Reads a DCML corpus of harmonic annotations.
 *
 * <p>Only the {@code harmonies/*.tsv} tables. The repositories also carry MuseScore scores,
 * and parsing those would mean handling a full engraving format to recover information the
 * TSV states directly and more reliably.
 *
 * <p>Two details of the format are easy to get wrong and are handled explicitly: positions
 * are exact fractions such as {@code 13/2}, and {@code localkey} is a Roman numeral
 * relative to the global key rather than an absolute key — {@code IV} in a piece in C means
 * F, not the key of IV.
 */
@ApplicationScoped
public class DcmlCorpusIngester implements HarmonyIngester {

    private static final Logger LOG = Logger.getLogger(DcmlCorpusIngester.class);
    private static final int PARSER_VERSION = 1;

    @Inject
    SourceFetcher fetcher;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "music.knowledge.dcml.max-files", defaultValue = "400")
    int maxFiles;

    /** Serves every DCML corpus; the pipeline picks it by ingestion mode and id prefix. */
    @Override
    public String sourceId() {
        return "dcml";
    }

    @Override
    public int parserVersion() {
        return PARSER_VERSION;
    }

    @Override
    public Harvest harvest(SourceManifest.ManifestSource source, int generation) {
        List<String> files = listHarmonyFiles(source);
        Map<String, PieceMetadata> metadata = readMetadata(source);
        List<HarmonyEvent> events = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        int works = 0;

        for (String path : files.stream().limit(maxFiles).toList()) {
            try {
                String tsv = fetcher.get(source.id(), rawUrl(source, path));
                int before = events.size();
                events.addAll(readTable(new TsvTable(tsv), source, path, generation, metadata));
                if (events.size() > before) {
                    works++;
                }
            } catch (RuntimeException e) {
                problems.add(path + ": " + e.getMessage());
                LOG.warnf("Skipping %s in %s: %s", path, source.id(), e.toString());
            }
        }
        LOG.infof("%s: %d harmony events from %d of %d files", source.id(), events.size(), works, files.size());
        return new Harvest(events, works, files.size(), problems);
    }

    List<HarmonyEvent> readTable(TsvTable table, SourceManifest.ManifestSource source,
                                 String path, int generation, Map<String, PieceMetadata> metadata) {
        List<HarmonyEvent> events = new ArrayList<>();
        String piece = pieceIdFrom(path);
        PieceMetadata about = metadata.get(piece);
        for (TsvTable.Row row : table.rows()) {
            String chord = row.text("chord");
            if (chord == null) {
                continue;
            }
            HarmonyEvent event = new HarmonyEvent();
            event.sourceId = source.id();
            event.generation = generation;
            event.composer = about != null && about.composer() != null
                    ? about.composer()
                    : (source.composer() == null ? source.name() : source.composer());
            event.work = about != null && about.workTitle() != null ? about.workTitle() : piece;
            event.movement = about == null ? null : about.movementLabel();
            event.measure = row.integer("mn");
            event.beat = row.fraction("mn_onset");
            event.globalKey = globalKey(row);
            event.localKey = row.text("localkey");
            event.romanNumeral = chord;
            event.chordLabel = row.text("label");
            event.chordType = row.text("chord_type");
            event.figbass = row.text("figbass");
            event.relativeRoot = row.text("relativeroot");
            event.cadence = row.text("cadence");
            event.phraseEnd = row.text("phraseend") != null;
            event.sourceReference = path + (event.measure == null ? "" : ", m. " + event.measure);
            event.licenseId = source.license();
            events.add(event);
        }
        return events;
    }

    /** The global key, with its mode, e.g. {@code C} major or {@code f} minor as published. */
    private static String globalKey(TsvTable.Row row) {
        String key = row.text("globalkey");
        if (key == null) {
            return null;
        }
        Boolean minor = row.flag("globalkey_is_minor");
        return Boolean.TRUE.equals(minor) ? key + " minor" : key + " major";
    }

    /**
     * The first usable title, cleaned.
     *
     * <p>Corpora carry titles taken straight from the engraving software, so a title_text
     * can arrive as {@code "<font size=""18""/>DREI QUARTETTE}. That is the publisher's data
     * and worth using, but not worth showing a learner as it stands.
     */
    private static String firstPresent(String... values) {
        for (String value : values) {
            String cleaned = cleanTitle(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    static String cleanTitle(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value
                .replaceAll("<[^>]*>", " ")     // markup from the score's title frame
                .replace("\"\"", "")
                .replace("\"", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String pieceIdFrom(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        return file.replace(".harmonies.tsv", "");
    }

    /**
     * The published title of a piece, so a citation reads "Sonata no. 1, Allegro, bar 17"
     * rather than "02-1, 1, bar 17". Corpora ship this as a metadata table at their root;
     * a corpus without one still ingests, and cites by file name.
     */
    record PieceMetadata(String composer, String workTitle, String movementTitle, String movementNumber) {

        String movementLabel() {
            if (movementTitle != null && movementTitle.equals(workTitle)) {
                return null;
            }
            if (movementTitle == null || movementTitle.isBlank()) {
                return movementNumber == null ? null : "movement " + movementNumber;
            }
            return movementNumber == null || movementNumber.isBlank()
                    ? movementTitle
                    : movementTitle + " (movement " + movementNumber + ")";
        }
    }

    Map<String, PieceMetadata> readMetadata(SourceManifest.ManifestSource source) {
        try {
            TsvTable table = new TsvTable(fetcher.get(source.id(), rawUrl(source, "metadata.tsv")));
            if (!table.hasColumn("piece")) {
                return Map.of();
            }
            Map<String, PieceMetadata> pieces = new java.util.HashMap<>();
            for (TsvTable.Row row : table.rows()) {
                String piece = row.text("piece");
                if (piece != null) {
                    // Corpora differ: some fill workTitle, some leave it blank and put the
                    // whole name in title_text. Falling through to the file id gives a
                    // citation like "n01op18-1_03", which nobody can look up.
                    String work = firstPresent(row.text("workTitle"), row.text("title_text"));
                    pieces.put(piece, new PieceMetadata(row.text("composer"), work,
                            row.text("movementTitle"), row.text("movementNumber")));
                }
            }
            return pieces;
        } catch (RuntimeException noMetadata) {
            LOG.infof("%s ships no metadata table; citing by file name", source.id());
            return Map.of();
        }
    }

    /**
     * Lists the harmony tables in the repository without cloning it. The tree endpoint is
     * one request; cloning twelve corpora of MuseScore scores to read their TSVs would not
     * be a reasonable thing to do to somebody's laptop.
     */
    List<String> listHarmonyFiles(SourceManifest.ManifestSource source) {
        String repository = source.repository();
        String slug = repository.substring(repository.indexOf("github.com/") + "github.com/".length())
                .replaceAll("/+$", "");
        String branch = source.branch() == null ? "main" : source.branch();
        String url = "https://api.github.com/repos/" + slug + "/git/trees/" + branch + "?recursive=1";
        try {
            JsonNode tree = objectMapper.readTree(fetcher.get(source.id(), url)).path("tree");
            List<String> files = new ArrayList<>();
            tree.forEach(node -> {
                String path = node.path("path").asText("");
                if (path.startsWith("harmonies/") && path.endsWith(".tsv")) {
                    files.add(path);
                }
            });
            files.sort(String::compareTo);
            return files;
        } catch (Exception e) {
            throw new IllegalStateException("Could not list " + source.id() + ": " + e.getMessage(), e);
        }
    }

    private static String rawUrl(SourceManifest.ManifestSource source, String path) {
        String repository = source.repository();
        String slug = repository.substring(repository.indexOf("github.com/") + "github.com/".length())
                .replaceAll("/+$", "");
        String branch = source.branch() == null ? "main" : source.branch();
        return "https://raw.githubusercontent.com/" + slug + "/" + branch + "/" + path;
    }
}
