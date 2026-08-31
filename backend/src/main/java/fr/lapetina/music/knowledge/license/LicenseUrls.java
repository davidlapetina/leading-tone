package fr.lapetina.music.knowledge.license;

import java.util.Map;
import java.util.Optional;

/**
 * The closed set of licence URLs this application recognises.
 *
 * <p>A source document states its licence as a URL. This maps that URL onto a licence
 * identifier in the manifest's vocabulary, and <strong>anything unmapped is unknown</strong>.
 * There is no optimistic fallback: an unrecognised licence URL means the material is not
 * ingested, and a line in the log saying so.
 *
 * <p>That rule is what makes the per-document licence check general rather than a list of
 * exceptions. Open Music Theory has one All Rights Reserved chapter today; if a new one
 * appears tomorrow under a licence nobody has considered, it is refused automatically
 * rather than silently inheriting the book's terms.
 */
public final class LicenseUrls {

    private static final Map<String, String> KNOWN = Map.of(
            "creativecommons.org/licenses/by/4.0", "CC-BY-4.0",
            "creativecommons.org/licenses/by-sa/4.0", "CC-BY-SA-4.0",
            "creativecommons.org/licenses/by-nc/4.0", "CC-BY-NC-4.0",
            "creativecommons.org/licenses/by-nc-sa/4.0", "CC-BY-NC-SA-4.0",
            "creativecommons.org/publicdomain/zero/1.0", "CC0-1.0",
            "creativecommons.org/publicdomain/mark/1.0", "PUBLIC-DOMAIN",
            "choosealicense.com/no-license", "ALL-RIGHTS-RESERVED");

    private LicenseUrls() {}

    /**
     * The licence identifier for a URL, or empty when the URL is not one we recognise.
     * Scheme, {@code www.}, trailing slashes and {@code legalcode} suffixes are ignored,
     * because publishers write the same licence a dozen ways.
     */
    public static Optional<String> identify(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String key = url.trim().toLowerCase()
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "")
                .replaceFirst("/?(deed|legalcode)(\\.[a-z-]+)?/?$", "")
                .replaceFirst("/+$", "");
        return Optional.ofNullable(KNOWN.get(key));
    }

    /** True when the URL names a licence that permits us to ingest the material at all. */
    public static boolean isIngestable(String url) {
        return identify(url).filter(id -> !"ALL-RIGHTS-RESERVED".equals(id)).isPresent();
    }
}
