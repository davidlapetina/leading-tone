package fr.lapetina.music.knowledge.license;

import fr.lapetina.music.knowledge.source.KnowledgeSource;
import fr.lapetina.music.knowledge.source.SourceManifest;
import fr.lapetina.music.knowledge.source.SourceRegistry;
import fr.lapetina.music.settings.SettingsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides what may be ingested and what may be retrieved, from the licence alone.
 *
 * <p>The rule this exists to enforce: <strong>this application's MIT licence says nothing
 * about anybody else's work.</strong> Open Music Theory stays CC BY-SA 4.0, the DCML
 * corpora and the Jazz Harmony Treebank stay CC BY-NC-SA 4.0, and no amount of
 * downloading, normalising, chunking, embedding or indexing moves a source onto our terms.
 * Attribution and ShareAlike obligations survive every one of those transformations.
 *
 * <p>The checks are in software rather than in documentation because a rule that lives
 * only in a README is a rule that gets forgotten during the next refactor.
 */
@ApplicationScoped
public class LicensePolicyService {

    @Inject
    SourceRegistry registry;

    @Inject
    SettingsService settingsService;

    public LicensePolicyService() {
        // For CDI.
    }

    /** For tests and tooling, which need the policy without starting the container. */
    public LicensePolicyService(SourceRegistry registry) {
        this.registry = registry;
    }

    /** Whether the terms of this licence are established at all. */
    public boolean isLicenseKnown(SourceLicense license) {
        return license != null && !"UNKNOWN".equals(license.id());
    }

    /**
     * Whether material under this licence may be brought in.
     *
     * <p>Unknown and rejected licences are refused. Guessing is not an option: the cost of
     * a wrong guess falls on the author whose work we would be redistributing.
     */
    public boolean canIngest(SourceLicense license) {
        if (!isLicenseKnown(license)) {
            return false;
        }
        LicenseStatus status = license.status();
        return status == LicenseStatus.VERIFIED || status == LicenseStatus.RESTRICTED;
    }

    /** Whether a document stating this licence URL may be brought in. */
    public boolean canIngestUrl(String licenseUrl) {
        return LicenseUrls.identify(licenseUrl).map(id -> canIngest(registry.licenseFor(id))).orElse(false);
    }

    /** Resolves a document's stated licence URL onto the vocabulary, or unknown. */
    public SourceLicense resolve(String licenseUrl) {
        return LicenseUrls.identify(licenseUrl).map(registry::licenseFor).orElseGet(SourceLicense::unknown);
    }

    /** What this deployment is, which decides what its NonCommercial sources permit. */
    public RuntimeMode currentMode() {
        if (settingsService == null) {
            return RuntimeMode.NON_COMMERCIAL;
        }
        return RuntimeMode.parse(settingsService.current().runtimeMode);
    }

    /**
     * Whether a source's material may be served, in the mode this deployment is running in.
     *
     * <p>Three things have to hold: the ingestion finished, the licence is one we accept at
     * all, and the licence permits the use this deployment is making of it. A half-ingested
     * source serves nothing rather than part of itself, and a NonCommercial source serves
     * nothing in a commercial deployment.
     */
    public boolean canRetrieve(KnowledgeSource source) {
        return canRetrieve(source, currentMode());
    }

    /** The same decision against an explicit mode, so it can be tested without settings. */
    public boolean canRetrieve(KnowledgeSource source, RuntimeMode mode) {
        if (source == null || !source.isRetrievable()) {
            return false;
        }
        SourceLicense license = registry.licenseFor(source.licenseId);
        if (!canIngest(license)) {
            return false;
        }
        return !mode.isCommercial() || license.commercialUseAllowed();
    }

    /**
     * The single list of sources anything may serve from.
     *
     * <p>Every retrieval path goes through here: lexical search, vector search, structured
     * harmony queries, and the score renderer. That is deliberate. This rule existed in two
     * copies and the score renderer had none, which is how a licence restriction quietly
     * stops applying to one route while the tests keep passing on the others.
     */
    public Set<String> retrievableSourceIds() {
        RuntimeMode mode = currentMode();
        return KnowledgeSource.<KnowledgeSource>listAll().stream()
                .filter(source -> canRetrieve(source, mode))
                .map(source -> source.id)
                .collect(Collectors.toSet());
    }

    /** Whether one named source may be served right now. Used by the score renderer. */
    public boolean canRetrieve(String sourceId) {
        return canRetrieve(KnowledgeSource.byId(sourceId));
    }

    /** Refuses with a reason, for callers that must not silently return nothing. */
    public void requireRetrievable(String sourceId) {
        if (!canRetrieve(sourceId)) {
            throw new IllegalStateException(
                    "Source " + sourceId + " is not available in " + currentMode() + " mode");
        }
    }

    public boolean requiresAttribution(SourceLicense license) {
        return license != null && license.attributionRequired();
    }

    public boolean requiresShareAlike(SourceLicense license) {
        return license != null && license.shareAlikeRequired();
    }

    /**
     * Recorded for the notices file, not enforced at runtime: this deployment is
     * non-commercial. Both the DCML corpora and the Jazz Harmony Treebank are
     * NonCommercial, so a commercial deployment would have to obtain separate permission
     * from the rights holders before enabling them.
     */
    public boolean allowsCommercialUse(SourceLicense license) {
        return license != null && license.commercialUseAllowed();
    }

    /** Throws with a reason, for the ingestion pipeline's licence gate. */
    public void requireIngestable(String sourceId, SourceLicense license) {
        if (!canIngest(license)) {
            throw new IllegalStateException(
                    "Refusing to ingest " + sourceId + ": licence is "
                            + (isLicenseKnown(license) ? license.id() + " (" + license.status() + ")" : "unknown"));
        }
    }

    public Optional<SourceManifest.ManifestSource> declaration(String sourceId) {
        return registry.find(sourceId);
    }
}
