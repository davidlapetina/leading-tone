package fr.lapetina.music.knowledge.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import fr.lapetina.music.knowledge.license.LicenseStatus;
import fr.lapetina.music.knowledge.license.SourceLicense;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and validates {@code knowledge-sources.yaml}, the authoritative declaration of
 * every external knowledge source.
 *
 * <p>Nothing is downloadable unless it is declared here. Validation is strict and happens
 * at startup, in the same spirit as {@code ConceptGraph}: a manifest naming a licence that
 * is not in the vocabulary, or two sources sharing an id, stops the application rather
 * than producing a subtly wrong attribution later.
 */
@ApplicationScoped
public class SourceRegistry {

    static final String MANIFEST = "knowledge-sources.yaml";

    private SourceManifest manifest;
    private Map<String, SourceLicense> licenses;

    @PostConstruct
    void init() {
        load();
    }

    /** Exposed so a test can load the shipped manifest without starting the container. */
    public void load() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(MANIFEST)) {
            if (in == null) {
                throw new IllegalStateException(MANIFEST + " is missing from the classpath");
            }
            index(mapper.readValue(in, SourceManifest.class));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + MANIFEST, e);
        }
    }

    void index(SourceManifest loaded) {
        Map<String, SourceLicense> vocabulary = loaded.licenceById();
        for (SourceManifest.ManifestSource source : loaded.sources()) {
            if (source.id() == null || source.id().isBlank()) {
                throw new IllegalStateException("A source in " + MANIFEST + " has no id");
            }
            if (loaded.sources().stream().filter(other -> source.id().equals(other.id())).count() > 1) {
                throw new IllegalStateException("Duplicate source id in " + MANIFEST + ": " + source.id());
            }
            if (!vocabulary.containsKey(source.license())) {
                throw new IllegalStateException(
                        "Source " + source.id() + " names licence " + source.license()
                                + ", which is not in the licence vocabulary of " + MANIFEST);
            }
            if (source.ingestionMode() == null) {
                throw new IllegalStateException("Source " + source.id() + " has no ingestionMode");
            }
        }
        this.manifest = loaded;
        this.licenses = vocabulary;
    }

    public List<SourceManifest.ManifestSource> all() {
        return manifest.sources();
    }

    public Optional<SourceManifest.ManifestSource> find(String id) {
        return manifest.source(id);
    }

    public SourceManifest.ManifestSource require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown knowledge source: " + id));
    }

    /** True when the id names a source we are allowed to fetch. Used to refuse arbitrary URLs. */
    public boolean isDeclared(String id) {
        return find(id).isPresent();
    }

    public SourceLicense licenseFor(String licenseId) {
        return licenses.getOrDefault(licenseId, SourceLicense.unknown());
    }

    /** The licence a source declares, which is only a default for its individual documents. */
    public SourceLicense licenseOf(SourceManifest.ManifestSource source) {
        return licenseFor(source.license());
    }

    public LicenseStatus statusOf(SourceManifest.ManifestSource source) {
        return licenseOf(source).status();
    }

    public Map<String, SourceLicense> vocabulary() {
        return licenses;
    }
}
