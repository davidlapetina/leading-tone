package fr.lapetina.music.knowledge.ingestion;

/**
 * Fetches bytes from a declared source.
 *
 * <p>An interface so that ingestion can be tested against recorded fixtures: the test
 * suite must never need the network, and an ingester whose parsing can only be exercised
 * by hitting a live website is an ingester nobody will change.
 */
public interface SourceFetcher {

    /**
     * @param sourceId the manifest id this fetch is on behalf of, so the implementation
     *     can refuse a URL that no declared source owns
     */
    String get(String sourceId, String url);
}
