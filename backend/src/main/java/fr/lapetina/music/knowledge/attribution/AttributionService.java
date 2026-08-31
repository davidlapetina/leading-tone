package fr.lapetina.music.knowledge.attribution;

import fr.lapetina.music.knowledge.chunk.KnowledgeChunk;
import fr.lapetina.music.knowledge.license.SourceLicense;
import fr.lapetina.music.knowledge.retrieval.RetrievedChunk;
import fr.lapetina.music.knowledge.source.SourceManifest;
import fr.lapetina.music.knowledge.source.SourceRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Says who a piece of knowledge belongs to.
 *
 * <p>Everything it returns comes from the manifest or from a stored row. It never composes
 * a plausible-looking reference: a citation nobody can check is worse than no citation,
 * because it looks like diligence.
 */
@ApplicationScoped
public class AttributionService {

    @Inject
    SourceRegistry registry;

    public Optional<Attribution> forSource(String sourceId) {
        return registry.find(sourceId).map(this::toAttribution);
    }

    public List<Attribution> all() {
        return registry.all().stream().map(this::toAttribution).toList();
    }

    /** The credits for a set of retrieved passages, deduplicated and in the order used. */
    public List<Attribution> forChunks(List<RetrievedChunk> chunks) {
        Set<String> sourceIds = new LinkedHashSet<>();
        chunks.forEach(chunk -> sourceIds.add(chunk.sourceId()));
        List<Attribution> credits = new ArrayList<>();
        for (String sourceId : sourceIds) {
            forSource(sourceId).ifPresent(credits::add);
        }
        return credits;
    }

    public Optional<Attribution> forChunkId(UUID chunkId) {
        KnowledgeChunk chunk = KnowledgeChunk.findById(chunkId);
        return chunk == null ? Optional.empty() : forSource(chunk.sourceId);
    }

    private Attribution toAttribution(SourceManifest.ManifestSource source) {
        SourceLicense license = registry.licenseOf(source);
        return new Attribution(
                source.id(),
                source.name(),
                license.id(),
                license.name(),
                license.url(),
                source.primaryUrl(),
                source.citation(),
                license.attributionRequired(),
                license.shareAlikeRequired(),
                license.commercialUseAllowed());
    }
}
