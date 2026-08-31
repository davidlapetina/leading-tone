package fr.lapetina.music.knowledge.ingestion;

import fr.lapetina.music.knowledge.source.SourceManifest;
import java.util.List;

/**
 * Reads one kind of source into documents.
 *
 * <p>An ingester is responsible for the licence decision about each individual document it
 * returns, because only it knows where that document states its terms. The pipeline around
 * it takes what it is given on trust and refuses to activate anything that failed.
 */
public interface Ingester {

    String sourceId();

    /** Bumped when parsing changes, so that existing material is re-parsed rather than kept. */
    int parserVersion();

    /** Documents fit to keep, with those whose licence forbids it already excluded. */
    Harvest harvest(SourceManifest.ManifestSource source);

    /**
     * @param skippedForLicense human-readable names of what was refused, so the report can
     *     say which material is missing and why
     */
    record Harvest(List<DocumentDraft> documents, int seen, int skippedEmpty, List<String> skippedForLicense) {
        public Harvest {
            documents = documents == null ? List.of() : List.copyOf(documents);
            skippedForLicense = skippedForLicense == null ? List.of() : List.copyOf(skippedForLicense);
        }
    }
}
