package fr.lapetina.music.knowledge.ingestion;

import fr.lapetina.music.knowledge.source.SourceManifest;
import fr.lapetina.music.knowledge.source.SourceRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The real fetcher, with the restrictions that keep ingestion from becoming a way to make
 * this server fetch arbitrary URLs.
 *
 * <p>A URL is only fetched when its host matches the host a declared source names. There
 * is no endpoint that takes a URL, and no configuration that adds one: adding a source
 * means editing {@code knowledge-sources.yaml} and having the change reviewed.
 *
 * <p>The user agent is not cosmetic. The Pressbooks API answers the default JDK agent with
 * HTTP 403.
 */
@ApplicationScoped
public class HttpSourceFetcher implements SourceFetcher {

    private static final Logger LOG = Logger.getLogger(HttpSourceFetcher.class);
    private static final int MAX_BYTES = 32 * 1024 * 1024;

    @Inject
    SourceRegistry registry;

    @Inject
    RawStore rawStore;

    @ConfigProperty(name = "music.knowledge.user-agent", defaultValue = "LeadingTone/1.0")
    String userAgent;

    @ConfigProperty(name = "music.knowledge.http-timeout-seconds", defaultValue = "30")
    int timeoutSeconds;

    @ConfigProperty(name = "music.knowledge.politeness-millis", defaultValue = "400")
    long politenessMillis;

    private volatile HttpClient client;
    private volatile long lastRequestAt;

    @Override
    public String get(String sourceId, String url) {
        return get(sourceId, url, false);
    }

    /**
     * @param refresh true to go back to the publisher even when a local copy exists. False
     *     is right for almost everything: the copy is what makes a rebuild fast and offline.
     */
    public String get(String sourceId, String url, boolean refresh) {
        requireDeclaredHost(sourceId, url);
        if (!refresh) {
            Optional<String> cached = rawStore.read(sourceId, url);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        String body = download(sourceId, url);
        rawStore.write(sourceId, url, body);
        return body;
    }

    private String download(String sourceId, String url) {
        pause();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json, text/plain, */*")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();
            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            }
            String body = response.body();
            if (body.length() > MAX_BYTES) {
                throw new IllegalStateException("Response from " + url + " is larger than the ingestion limit");
            }
            return body;
        } catch (IOException e) {
            throw new IllegalStateException("Could not fetch " + url + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching " + url, e);
        }
    }

    /**
     * Refuses any URL whose host is not the one the declared source publishes from. This is
     * the check that stops an ingestion endpoint being a request forgery primitive.
     */
    void requireDeclaredHost(String sourceId, String url) {
        SourceManifest.ManifestSource source = registry.require(sourceId);
        String host = hostOf(url).orElseThrow(
                () -> new IllegalArgumentException("Not a fetchable URL: " + url));
        boolean permitted = allowedHosts(source).stream().anyMatch(host::equalsIgnoreCase);
        if (!permitted) {
            throw new IllegalArgumentException(
                    "Refusing to fetch " + host + ": source " + sourceId + " does not declare that host");
        }
    }

    private static java.util.List<String> allowedHosts(SourceManifest.ManifestSource source) {
        return java.util.stream.Stream.of(source.sourceUrl(), source.apiBase(), source.repository())
                .filter(java.util.Objects::nonNull)
                .map(HttpSourceFetcher::hostOf)
                .flatMap(Optional::stream)
                .flatMap(host -> "github.com".equalsIgnoreCase(host)
                        // A GitHub repository is declared by its page; its file list comes
                        // from the API host and its contents from the raw host. Those three,
                        // and nothing else.
                        ? java.util.stream.Stream.of(host, "raw.githubusercontent.com", "api.github.com")
                        : java.util.stream.Stream.of(host))
                .toList();
    }

    private static Optional<String> hostOf(String url) {
        try {
            URI uri = URI.create(url);
            return Optional.ofNullable(uri.getHost());
        } catch (IllegalArgumentException notAUri) {
            return Optional.empty();
        }
    }

    /** A courtesy to the servers we are reading from, not a rate limit on ourselves. */
    private void pause() {
        long wait = politenessMillis - (System.currentTimeMillis() - lastRequestAt);
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    private HttpClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .build();
                    LOG.debugf("Knowledge fetcher ready, user agent %s", userAgent);
                }
            }
        }
        return client;
    }
}
