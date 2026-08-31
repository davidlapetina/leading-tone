package fr.lapetina.music.api;

import fr.lapetina.music.knowledge.attribution.Attribution;
import fr.lapetina.music.knowledge.attribution.AttributionService;
import fr.lapetina.music.knowledge.chunk.KnowledgeChunk;
import fr.lapetina.music.knowledge.chunk.KnowledgeDocument;
import fr.lapetina.music.knowledge.embedding.EmbeddingService;
import fr.lapetina.music.knowledge.harmony.HarmonyEvent;
import fr.lapetina.music.knowledge.index.KnowledgeIndex;
import fr.lapetina.music.knowledge.ingestion.IngestReport;
import fr.lapetina.music.knowledge.ingestion.IngestionService;
import fr.lapetina.music.knowledge.license.SourceLicense;
import fr.lapetina.music.knowledge.retrieval.KnowledgeRetriever;
import fr.lapetina.music.knowledge.retrieval.RetrievalQuery;
import fr.lapetina.music.knowledge.retrieval.RetrievalResult;
import fr.lapetina.music.knowledge.source.KnowledgeSource;
import fr.lapetina.music.knowledge.source.SourceManifest;
import fr.lapetina.music.knowledge.source.SourceRegistry;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Administration and diagnostics for the knowledge layer.
 *
 * <p>Only sources named in {@code knowledge-sources.yaml} are addressable: there is no
 * endpoint that takes a URL or a filesystem path, so this cannot be used to make the
 * server fetch something nobody reviewed.
 */
@Path("/api/knowledge")
@Produces(MediaType.APPLICATION_JSON)
public class KnowledgeResource {

    @Inject
    SourceRegistry registry;

    @Inject
    IngestionService ingestionService;

    @Inject
    KnowledgeRetriever retriever;

    @Inject
    KnowledgeIndex index;

    @Inject
    AttributionService attributionService;

    @Inject
    EmbeddingService embedder;

    @Inject
    fr.lapetina.music.knowledge.harmony.ConceptExamples conceptExamples;

    @Inject
    fr.lapetina.music.knowledge.provenance.ProvenanceService provenanceService;

    @Inject
    fr.lapetina.music.knowledge.ingestion.RawStore rawStore;

    @GET
    @Path("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("indexOpen", index.isOpen());
        status.put("indexGeneration", index.activeGeneration());
        status.put("embeddingModel", embedder.isAvailable() ? embedder.info().signature() : "none");
        status.put("vectorSearch", index.isVectorCapable(
                embedder.isAvailable() ? embedder.info() : fr.lapetina.music.knowledge.embedding.EmbeddingModelInfo.NONE));
        status.put("documents", KnowledgeDocument.countActive());
        status.put("chunks", KnowledgeChunk.countActive());
        status.put("harmonyEvents", HarmonyEvent.countActive());
        status.put("declaredSources", registry.all().size());
        status.put("activeSources", KnowledgeSource.retrievable().size());
        index.meta().ifPresent(meta -> status.put("indexCreatedAt", meta.createdAt()));
        index.unavailableReason().ifPresent(reason -> status.put("unavailable", reason));
        return status;
    }

    @GET
    @Path("/sources")
    public List<Map<String, Object>> sources() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SourceManifest.ManifestSource declared : registry.all()) {
            out.add(describe(declared));
        }
        return out;
    }

    @GET
    @Path("/sources/{id}")
    public Map<String, Object> source(@PathParam("id") String id) {
        return describe(registry.require(id));
    }

    /**
     * Brings a source in. Nothing downloads at startup, so this is the only way material
     * arrives, and it only ever reaches hosts the manifest declares.
     */
    @POST
    @Path("/sources/{id}/ingest")
    public IngestReport ingest(@PathParam("id") String id, @QueryParam("force") @DefaultValue("false") boolean force) {
        registry.require(id);
        return ingestionService.ingest(id, force);
    }

    /**
     * Rebuilds from the local copy, without going back to the publisher.
     *
     * <p>This is what makes changing the chunk policy or the embedding model cheap: the
     * downloaded originals are kept, so a rebuild is a local operation.
     */
    @POST
    @Path("/sources/{id}/reindex")
    public IngestReport reindex(@PathParam("id") String id) {
        registry.require(id);
        return ingestionService.ingest(id, true);
    }

    /**
     * Discards the local copy and fetches the source again.
     *
     * <p>The one operation that genuinely needs the network. Separate from reindex because
     * "rebuild what I have" and "go and see whether it changed" are different intentions,
     * and only one of them should be reaching out to somebody else's server.
     */
    @POST
    @Path("/sources/{id}/refresh")
    public IngestReport refresh(@PathParam("id") String id) {
        registry.require(id);
        rawStore.forget(id);
        return ingestionService.ingest(id, true);
    }

    /**
     * Shows exactly what the tutor would be handed for a question, with scores. This is
     * the endpoint that makes a ranking argument settleable.
     */
    @GET
    @Path("/search")
    public Map<String, Object> search(@QueryParam("q") @Size(max = 2000) String query,
                                      @QueryParam("concept") String conceptId,
                                      @QueryParam("limit") @DefaultValue("5") int limit) {
        RetrievalResult result = retriever.retrieve(
                new RetrievalQuery(query, conceptId, Math.clamp(limit, 1, 20)));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("vectorUsed", result.vectorUsed());
        response.put("millis", result.millis());
        response.put("results", result.chunks().stream().map(chunk -> {
            Map<String, Object> hit = new LinkedHashMap<>();
            hit.put("chunkId", chunk.chunkId());
            hit.put("score", chunk.score());
            hit.put("lexical", chunk.lexicalScore());
            hit.put("vector", chunk.vectorScore());
            hit.put("citation", chunk.citation());
            hit.put("attribution", chunk.attribution());
            hit.put("license", chunk.licenseId());
            hit.put("url", chunk.url());
            hit.put("excerpt", chunk.body().length() > 400 ? chunk.body().substring(0, 400) + "…" : chunk.body());
            return hit;
        }).toList());
        response.put("sources", attributionService.forChunks(result.chunks()));
        return response;
    }

    /**
     * Real musical examples, found by querying annotations rather than by similarity.
     *
     * <p>An empty list is a real answer: it means no corpus we hold contains that chord in
     * that composer, and the honest response is to say so rather than to produce something
     * that sounds like Beethoven.
     */
    @GET
    @Path("/examples")
    public Map<String, Object> examples(@QueryParam("romanNumeral") @Size(max = 40) String romanNumeral,
                                        @QueryParam("composer") @Size(max = 80) String composer,
                                        @QueryParam("key") @Size(max = 40) String key,
                                        @QueryParam("cadence") @Size(max = 20) String cadence,
                                        @QueryParam("limit") @DefaultValue("5") int limit) {
        int capped = Math.clamp(limit, 1, 50);
        // Through the same path as /examples/for-concept, so both return one shape and both
        // arrive engraved. Two endpoints answering the same question differently is a bug
        // waiting for whichever caller trusted the other one.
        List<fr.lapetina.music.knowledge.harmony.MusicalExample> found =
                conceptExamples.forQuery(romanNumeral, cadence, composer, capped);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", Map.of("romanNumeral", romanNumeral == null ? "" : romanNumeral,
                "composer", composer == null ? "" : composer,
                "cadence", cadence == null ? "" : cadence));
        response.put("found", found.size());
        response.put("examples", found);
        if (found.isEmpty()) {
            response.put("note", "No verified example in the corpora currently ingested. "
                    + "Nothing is invented to fill the gap.");
        }
        return response;
    }

    /**
     * Real music illustrating a concept, engraved.
     *
     * <p>Returns an empty list rather than reaching for something loosely related. The
     * interface says so plainly; inventing a passage to fill the space is the failure this
     * whole subsystem exists to prevent.
     */
    @GET
    @Path("/examples/for-concept/{conceptId}")
    public Map<String, Object> examplesForConcept(@PathParam("conceptId") String conceptId,
                                                  @QueryParam("limit") @DefaultValue("2") int limit) {
        List<fr.lapetina.music.knowledge.harmony.MusicalExample> found =
                conceptExamples.forConcept(conceptId, Math.clamp(limit, 1, 8));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("conceptId", conceptId);
        response.put("found", found.size());
        response.put("examples", found);
        response.put("note", found.isEmpty()
                ? "No passage in the loaded scores is a clear example of this."
                : null);
        return response;
    }

    /**
     * What recent answers were built from.
     *
     * <p>Retrievals and calculations only. The model's reasoning is not recorded: it is not
     * observable, and storing a plausible rationalisation beside real evidence would make
     * the evidence harder to trust rather than easier.
     */
    @GET
    @Path("/provenance")
    public List<Map<String, Object>> provenance(@QueryParam("limit") @DefaultValue("20") int limit) {
        return provenanceService.recent(limit).stream().map(record -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("interactionId", record.interactionId);
            row.put("conceptId", record.conceptId);
            row.put("intents", record.intents);
            row.put("theoryOperations", record.theoryOperations);
            row.put("retrievedChunkIds", record.chunkIds);
            row.put("corpusExamples", record.harmonyEventIds);
            row.put("sourcesUsed", record.sourceIds);
            row.put("at", record.createdAt);
            return row;
        }).toList();
    }

    /**
     * Who everything belongs to. The application's own code is MIT; none of this is, and
     * the two are never merged into one claim.
     */
    @GET
    @Path("/attribution")
    public Map<String, Object> attribution() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("applicationLicense", "MIT");
        response.put("notice", "The application is MIT licensed. The knowledge sources below are not: "
                + "each keeps its own upstream licence, which ingestion does not change.");
        response.put("sources", attributionService.all());
        return response;
    }

    private Map<String, Object> describe(SourceManifest.ManifestSource declared) {
        SourceLicense license = registry.licenseOf(declared);
        KnowledgeSource stored = KnowledgeSource.byId(declared.id());
        Attribution credit = attributionService.forSource(declared.id()).orElse(null);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", declared.id());
        out.put("name", declared.name());
        out.put("url", declared.primaryUrl());
        out.put("ingestionMode", declared.ingestionMode());
        out.put("tradition", declared.tradition());
        out.put("enabled", declared.isEnabled());
        out.put("license", license.id());
        out.put("licenseName", license.name());
        out.put("licenseUrl", license.url());
        out.put("licenseStatus", license.status());
        out.put("attributionRequired", license.attributionRequired());
        out.put("shareAlikeRequired", license.shareAlikeRequired());
        out.put("commercialUseAllowed", license.commercialUseAllowed());
        out.put("citation", declared.citation());
        out.put("state", stored == null ? "DISCOVERED" : stored.state.name());
        out.put("retrievable", stored != null && stored.isRetrievable());
        out.put("documents", stored == null ? 0 : stored.documentCount);
        out.put("chunks", stored == null ? 0 : stored.chunkCount);
        out.put("lastIngestedAt", stored == null ? null : stored.retrievedAt);
        out.put("lastError", stored == null ? null : stored.lastError);
        out.put("cachedBytes", rawStore.sizeOf(declared.id()));
        if (credit != null) {
            out.put("credit", credit.shortCredit());
        }
        return out;
    }
}
