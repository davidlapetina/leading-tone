package fr.lapetina.music.knowledge.harmony;

import fr.lapetina.music.knowledge.index.KnowledgePaths;
import fr.lapetina.music.knowledge.ingestion.SourceFetcher;
import fr.lapetina.music.knowledge.ingestion.TsvTable;
import fr.lapetina.music.knowledge.source.SourceManifest;
import fr.lapetina.music.knowledge.source.SourceRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Fetches the notes of a piece so an excerpt can be engraved.
 *
 * <p>Note tables are fetched only when somebody actually looks at an excerpt, and then
 * cached on disk. Ingesting every note of twelve corpora up front would be tens of
 * megabytes to answer questions nobody has asked yet; the harmonic annotations are the part
 * worth having in the database.
 */
@ApplicationScoped
public class ScoreSource {

    private static final Logger LOG = Logger.getLogger(ScoreSource.class);

    @Inject
    SourceFetcher fetcher;

    @Inject
    SourceRegistry registry;

    @Inject
    KnowledgePaths paths;

    @Inject
    fr.lapetina.music.knowledge.license.LicensePolicyService licensePolicy;

    /**
     * The notes of one piece, from the cache when it is there and from the publisher
     * otherwise. Empty when the corpus publishes no note table, which is true of the Jazz
     * Harmony Treebank: it carries chord symbols and no notes at all.
     */
    public List<NoteEvent> notesFor(String sourceId, String reference, int fromMeasure, int toMeasure) {
        // Notation is source material like any other. Rendering it from a corpus this
        // deployment may not serve would be the same disclosure as quoting its text, just
        // harder to notice.
        if (!licensePolicy.canRetrieve(sourceId)) {
            LOG.debugf("Not rendering notation from %s: not available in %s mode",
                    sourceId, licensePolicy.currentMode());
            return List.of();
        }
        String piece = pieceFrom(reference);
        if (piece == null) {
            return List.of();
        }
        try {
            String tsv = cached(sourceId, piece);
            if (tsv == null) {
                return List.of();
            }
            return read(new TsvTable(tsv), fromMeasure, toMeasure);
        } catch (RuntimeException e) {
            LOG.debugf("No notes for %s/%s: %s", sourceId, piece, e.toString());
            return List.of();
        }
    }

    private List<NoteEvent> read(TsvTable table, int fromMeasure, int toMeasure) {
        List<NoteEvent> notes = new ArrayList<>();
        for (TsvTable.Row row : table.rows()) {
            Integer measure = row.integer("mn");
            String name = row.text("name");
            if (measure == null || name == null || measure < fromMeasure || measure > toMeasure) {
                continue;
            }
            Double onset = row.fraction("mn_onset");
            Double duration = row.fraction("duration");
            notes.add(new NoteEvent(measure,
                    onset == null ? 0.0 : onset,
                    duration == null ? 0.125 : duration,
                    orDefault(row.integer("staff"), 1),
                    orDefault(row.integer("voice"), 1),
                    name,
                    row.text("tied") != null,
                    row.text("timesig")));
        }
        return notes;
    }

    /** The published notes for a piece, downloaded once and kept. */
    private String cached(String sourceId, String piece) {
        Path file = paths.rawFor(sourceId).resolve("notes").resolve(piece + ".notes.tsv");
        try {
            if (Files.exists(file)) {
                return Files.readString(file);
            }
            SourceManifest.ManifestSource source = registry.require(sourceId);
            if (source.repository() == null) {
                return null;
            }
            String tsv = fetcher.get(sourceId, rawUrl(source, "notes/" + piece + ".notes.tsv"));
            paths.ensure(file.getParent());
            Files.writeString(file, tsv);
            return tsv;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The harmony row records where it came from; the piece id is the file it names. */
    static String pieceFrom(String reference) {
        if (reference == null || !reference.contains("harmonies/")) {
            return null;
        }
        String path = reference.substring(reference.indexOf("harmonies/") + "harmonies/".length());
        int comma = path.indexOf(',');
        if (comma >= 0) {
            path = path.substring(0, comma);
        }
        return path.replace(".harmonies.tsv", "").trim();
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static String rawUrl(SourceManifest.ManifestSource source, String path) {
        String repository = source.repository();
        String slug = repository.substring(repository.indexOf("github.com/") + "github.com/".length())
                .replaceAll("/+$", "");
        String branch = source.branch() == null ? "main" : source.branch();
        return "https://raw.githubusercontent.com/" + slug + "/" + branch + "/" + path;
    }
}
