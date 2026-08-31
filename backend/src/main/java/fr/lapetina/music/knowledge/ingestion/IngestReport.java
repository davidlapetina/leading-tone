package fr.lapetina.music.knowledge.ingestion;

import fr.lapetina.music.knowledge.source.SourceState;
import java.util.List;

/**
 * What one ingestion run did.
 *
 * <p>{@code skippedForLicense} is reported rather than merely logged: a run that quietly
 * declined to read part of a source should say so, because "we have all of Open Music
 * Theory" and "we have all of it except two chapters" are different claims.
 */
public record IngestReport(
        String sourceId,
        SourceState state,
        int generation,
        boolean skipped,
        int documentsSeen,
        int documentsIngested,
        int documentsSkippedLicense,
        int documentsSkippedEmpty,
        int chunksWritten,
        int harmonyWritten,
        List<String> skippedForLicense,
        String message) {

    public IngestReport {
        skippedForLicense = skippedForLicense == null ? List.of() : List.copyOf(skippedForLicense);
    }

    public static IngestReport failed(String sourceId, String message) {
        return new IngestReport(sourceId, SourceState.FAILED, 0, false, 0, 0, 0, 0, 0, 0, List.of(), message);
    }

    public boolean isSuccess() {
        return state == SourceState.ACTIVE;
    }
}
