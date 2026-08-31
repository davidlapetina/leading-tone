package fr.lapetina.music.knowledge.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.lapetina.music.knowledge.license.SourceLicense;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The parsed form of {@code knowledge-sources.yaml}.
 *
 * <p>The manifest is the authority on what exists and what its terms are. The database
 * holds ingestion state, which changes; this holds the declaration, which does not change
 * except by someone editing the file deliberately.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SourceManifest(List<ManifestSource> sources, List<ManifestLicense> licenses) {

    public SourceManifest {
        sources = sources == null ? List.of() : List.copyOf(sources);
        licenses = licenses == null ? List.of() : List.copyOf(licenses);
    }

    public Map<String, SourceLicense> licenceById() {
        return licenses.stream()
                .map(ManifestLicense::toLicense)
                .collect(Collectors.toMap(SourceLicense::id, Function.identity()));
    }

    public Optional<ManifestSource> source(String id) {
        return sources.stream().filter(source -> source.id().equals(id)).findFirst();
    }

    /** One declared source. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ManifestSource(
            String id,
            String name,
            String version,
            String description,
            String sourceUrl,
            String apiBase,
            String repository,
            String branch,
            String dataFile,
            String publisher,
            String composer,
            List<String> authors,
            List<String> editors,
            String citation,
            String license,
            IngestionMode ingestionMode,
            Tradition tradition,
            Boolean enabled,
            Boolean perResourceLicense) {

        public ManifestSource {
            authors = authors == null ? List.of() : List.copyOf(authors);
            editors = editors == null ? List.of() : List.copyOf(editors);
        }

        public boolean isEnabled() {
            return enabled == null || enabled;
        }

        /**
         * Whether individual resources inside this source carry their own licences that
         * must be checked rather than inherited. True for Open Music Theory, where two of
         * its 140 chapters are not under the book's licence.
         */
        public boolean hasPerResourceLicense() {
            return Boolean.TRUE.equals(perResourceLicense);
        }

        /** The address the source is fetched from, whichever form it was declared in. */
        public String primaryUrl() {
            return sourceUrl != null ? sourceUrl : repository;
        }
    }

    /** One entry in the licence vocabulary. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ManifestLicense(
            String id,
            String name,
            String url,
            String text,
            Boolean attributionRequired,
            Boolean shareAlikeRequired,
            Boolean derivativesAllowed,
            Boolean commercialUseAllowed,
            Boolean ingestible) {

        public SourceLicense toLicense() {
            return new SourceLicense(
                    id,
                    name,
                    url,
                    text,
                    !Boolean.FALSE.equals(attributionRequired),
                    Boolean.TRUE.equals(shareAlikeRequired),
                    !Boolean.FALSE.equals(derivativesAllowed),
                    Boolean.TRUE.equals(commercialUseAllowed),
                    !Boolean.FALSE.equals(ingestible));
        }
    }
}
