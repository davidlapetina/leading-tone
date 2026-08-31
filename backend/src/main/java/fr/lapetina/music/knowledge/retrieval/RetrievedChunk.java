package fr.lapetina.music.knowledge.retrieval;

/**
 * One passage the tutor may quote, with everything needed to credit it.
 *
 * <p>There is no constructor that omits the attribution. A passage that cannot say where
 * it came from is not something this application is willing to put in front of a learner.
 */
public record RetrievedChunk(
        String chunkId,
        String documentId,
        String sourceId,
        String documentTitle,
        String sectionTitle,
        String body,
        String attribution,
        String licenseId,
        String url,
        double score,
        double lexicalScore,
        double vectorScore) {

    public String citation() {
        String section = sectionTitle == null || sectionTitle.isBlank() ? documentTitle
                : documentTitle + " — " + sectionTitle;
        return section == null ? sourceId : section;
    }
}
