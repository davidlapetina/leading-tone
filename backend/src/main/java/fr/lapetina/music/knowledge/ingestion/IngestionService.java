package fr.lapetina.music.knowledge.ingestion;

import fr.lapetina.music.knowledge.chunk.KnowledgeChunk;
import fr.lapetina.music.knowledge.chunk.KnowledgeDocument;
import fr.lapetina.music.knowledge.embedding.EmbeddingModelInfo;
import fr.lapetina.music.knowledge.embedding.EmbeddingService;
import fr.lapetina.music.knowledge.index.IndexBuilder;
import fr.lapetina.music.knowledge.index.KnowledgeIndex;
import fr.lapetina.music.knowledge.index.KnowledgePaths;
import fr.lapetina.music.knowledge.index.MusicAnalyzer;
import fr.lapetina.music.knowledge.license.LicensePolicyService;
import fr.lapetina.music.knowledge.license.SourceLicense;
import fr.lapetina.music.knowledge.source.KnowledgeSource;
import fr.lapetina.music.knowledge.source.SourceManifest;
import fr.lapetina.music.knowledge.source.SourceRegistry;
import fr.lapetina.music.knowledge.source.SourceState;
import fr.lapetina.music.knowledge.text.ChunkPolicy;
import fr.lapetina.music.knowledge.text.Chunker;
import fr.lapetina.music.knowledge.text.ConceptTagger;
import fr.lapetina.music.knowledge.text.HtmlSection;
import fr.lapetina.music.knowledge.text.SectionSplitter;
import fr.lapetina.music.knowledge.text.TextChunk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Brings a source in, or leaves everything exactly as it was.
 *
 * <p>The pipeline is licence check, harvest, persist, index, activate. Each new run writes
 * a complete new generation of documents, chunks and index while the previous generation
 * keeps serving searches; only the last step switches over, in one transaction. So a run
 * that fails at any point leaves a working application behind it, and there is no state in
 * which half a source is searchable.
 *
 * <p>Ingestion reaches the network, so it never happens at startup. It is something
 * somebody asks for.
 */
@ApplicationScoped
public class IngestionService {

    private static final Logger LOG = Logger.getLogger(IngestionService.class);

    @Inject
    SourceRegistry registry;

    @Inject
    LicensePolicyService licensePolicy;

    @Inject
    Instance<Ingester> ingesters;

    @Inject
    Instance<HarmonyIngester> harmonyIngesters;

    @Inject
    ConceptTagger conceptTagger;

    @Inject
    IndexBuilder indexBuilder;

    @Inject
    KnowledgeIndex index;

    @Inject
    KnowledgePaths paths;

    @Inject
    EmbeddingService embedder;

    @ConfigProperty(name = "music.knowledge.allow-ingest", defaultValue = "false")
    boolean allowIngest;

    @ConfigProperty(name = "music.knowledge.chunk.target-chars", defaultValue = "1200")
    int targetChars;

    @ConfigProperty(name = "music.knowledge.chunk.max-chars", defaultValue = "1800")
    int maxChars;

    @ConfigProperty(name = "music.knowledge.chunk.min-chars", defaultValue = "250")
    int minChars;

    public IngestReport ingest(String sourceId, boolean force) {
        if (!allowIngest) {
            throw new IllegalStateException(
                    "Ingestion is switched off. Set music.knowledge.allow-ingest to enable it.");
        }
        SourceManifest.ManifestSource declaration = registry.require(sourceId);
        SourceLicense license = registry.licenseOf(declaration);

        // The licence gate comes first, before anything is fetched. A source whose terms
        // are unknown or refused never reaches the network, let alone the index.
        if (!licensePolicy.canIngest(license)) {
            String reason = "licence " + license.id() + " (" + license.status() + ") does not permit ingestion";
            recordFailure(sourceId, reason);
            return IngestReport.failed(sourceId, reason);
        }
        if (!declaration.isEnabled()) {
            recordState(sourceId, SourceState.DISABLED, null);
            return IngestReport.failed(sourceId, "source is disabled in the manifest");
        }

        try {
            if (declaration.ingestionMode() == fr.lapetina.music.knowledge.source.IngestionMode.STRUCTURED_HARMONY) {
                return runHarmony(declaration, force);
            }
            Ingester ingester = ingesterFor(sourceId)
                    .orElseThrow(() -> new IllegalStateException("No ingester is registered for " + sourceId));
            return run(declaration, license, ingester, force);
        } catch (RuntimeException e) {
            LOG.errorf(e, "Ingestion of %s failed", sourceId);
            recordFailure(sourceId, e.getMessage());
            return IngestReport.failed(sourceId, e.getMessage());
        }
    }

    private IngestReport run(SourceManifest.ManifestSource declaration, SourceLicense license,
                             Ingester ingester, boolean force) {
        String sourceId = declaration.id();
        // Read before anything is written. recordState mutates the managed row, so asking it
        // afterwards what state the source *was* in answers with the state this run just set.
        Previous previous = Previous.of(KnowledgeSource.byId(sourceId));

        recordState(sourceId, SourceState.LICENSE_VERIFIED, null);

        Ingester.Harvest harvest = ingester.harvest(declaration);
        recordState(sourceId, SourceState.DOWNLOADED, null);

        EmbeddingModelInfo model = embedder.isAvailable() ? embedder.info() : EmbeddingModelInfo.NONE;
        String fingerprint = Checksums.fingerprint(
                declaration.version(),
                harvest.documents().stream().map(draft -> Checksums.of(draft.html())).toList(),
                ingester.parserVersion(),
                ChunkPolicy.CURRENT_VERSION,
                MusicAnalyzer.ANALYZER_VERSION,
                model.signature());

        if (!force && previous.isUnchanged(fingerprint)) {
            LOG.infof("%s is unchanged; nothing to re-embed or re-index", sourceId);
            recordState(sourceId, SourceState.ACTIVE, null);
            return new IngestReport(sourceId, SourceState.ACTIVE, previous.generation(), true,
                    harvest.seen(), previous.documents(), harvest.skippedForLicense().size(),
                    harvest.skippedEmpty(), previous.chunks(), previous.harmony(),
                    harvest.skippedForLicense(), "Already up to date");
        }

        int generation = previous.generation() + 1;
        int chunkCount = persist(declaration, license, harvest, generation);
        recordState(sourceId, SourceState.PARSED, null);

        List<KnowledgeChunk> forIndex = allActiveAndPending(sourceId, generation);
        indexBuilder.build(generation, forIndex, embedder);
        recordState(sourceId, SourceState.INDEXED, null);

        activate(sourceId, generation, harvest.documents().size(), chunkCount, fingerprint,
                ingester.parserVersion(), model.signature());
        paths.writeCurrent(generation);
        index.activate(generation);
        paths.pruneGenerationsBefore(generation - 1);

        LOG.infof("%s is active: generation %d, %d documents, %d chunks",
                sourceId, generation, harvest.documents().size(), chunkCount);
        return new IngestReport(sourceId, SourceState.ACTIVE, generation, false,
                harvest.seen(), harvest.documents().size(), harvest.skippedForLicense().size(),
                harvest.skippedEmpty(), chunkCount, 0, harvest.skippedForLicense(), null);
    }

    /**
     * A corpus of harmonic annotations. Same guarantees as the text path — nothing is
     * active until the whole run succeeds — but the output is rows to query rather than
     * passages to quote, so it never touches the Lucene index.
     */
    private IngestReport runHarmony(SourceManifest.ManifestSource declaration, boolean force) {
        String sourceId = declaration.id();
        Previous previous = Previous.of(KnowledgeSource.byId(sourceId));
        recordState(sourceId, SourceState.LICENSE_VERIFIED, null);

        // The treebank has its own reader; every DCML corpus shares one.
        HarmonyIngester ingester = harmonyIngesters.stream()
                .filter(candidate -> candidate.sourceId().equals(sourceId))
                .findFirst()
                .or(() -> harmonyIngesters.stream().filter(candidate -> sourceId.startsWith(candidate.sourceId())).findFirst())
                .orElseThrow(() -> new IllegalStateException("No harmony ingester handles " + sourceId));

        int generation = previous.generation() + 1;

        HarmonyIngester.Harvest harvest = ingester.harvest(declaration, generation);
        recordState(sourceId, SourceState.PARSED, null);

        String fingerprint = Checksums.fingerprint(
                declaration.version(),
                harvest.events().stream().map(event -> event.sourceReference + "|" + event.romanNumeral).toList(),
                ingester.parserVersion(), 0, 0, "structured");
        if (!force && previous.isUnchanged(fingerprint)) {
            recordState(sourceId, SourceState.ACTIVE, null);
            return new IngestReport(sourceId, SourceState.ACTIVE, previous.generation(), true,
                    harvest.filesSeen(), previous.documents(), 0, 0, 0, previous.harmony(),
                    List.of(), "Already up to date");
        }

        persistHarmony(harvest, generation);
        activateHarmony(sourceId, generation, harvest.worksSeen(), harvest.events().size(),
                fingerprint, ingester.parserVersion());

        LOG.infof("%s is active: %d harmony events from %d works",
                sourceId, harvest.events().size(), harvest.worksSeen());
        return new IngestReport(sourceId, SourceState.ACTIVE, generation, false,
                harvest.filesSeen(), harvest.worksSeen(), 0, 0, 0, harvest.events().size(),
                harvest.problems(), null);
    }

    @Transactional
    void persistHarmony(HarmonyIngester.Harvest harvest, int generation) {
        for (fr.lapetina.music.knowledge.harmony.HarmonyEvent event : harvest.events()) {
            event.persist();
        }
    }

    @Transactional
    void activateHarmony(String sourceId, int generation, int works, int events,
                         String fingerprint, int parserVersion) {
        fr.lapetina.music.knowledge.harmony.HarmonyEvent.update(
                "active = false where sourceId = ?1", sourceId);
        fr.lapetina.music.knowledge.harmony.HarmonyEvent.update(
                "active = true where sourceId = ?1 and generation = ?2", sourceId, generation);
        // Same reasoning as the text path: a corpus is tens of thousands of rows, and
        // keeping every past ingestion of it would be the largest thing on the disk.
        fr.lapetina.music.knowledge.harmony.HarmonyEvent.delete(
                "sourceId = ?1 and generation < ?2", sourceId, generation);

        KnowledgeSource source = KnowledgeSource.byId(sourceId);
        source.state = SourceState.ACTIVE;
        source.activeGeneration = generation;
        source.documentCount = works;
        source.harmonyCount = events;
        source.fingerprint = fingerprint;
        source.parserVersion = parserVersion;
        source.retrievedAt = Instant.now();
        source.lastError = null;
        source.updatedAt = Instant.now();
    }

    /**
     * What a source looked like before this run touched it.
     *
     * <p>A value, not the entity. The entity is managed, and the pipeline mutates it as it
     * advances, so holding a reference and asking later what state the source *was* in
     * answers with the state this run just set. That is not hypothetical: it silently
     * disabled the unchanged-source check, and every re-ingest re-embedded the whole source.
     */
    record Previous(SourceState state, String fingerprint, int generation,
                            int documents, int chunks, int harmony) {

        static Previous of(KnowledgeSource source) {
            return source == null
                    ? new Previous(SourceState.DISCOVERED, null, 0, 0, 0, 0)
                    : new Previous(source.state, source.fingerprint, source.activeGeneration,
                            source.documentCount, source.chunkCount, source.harmonyCount);
        }

        boolean isUnchanged(String candidate) {
            return state == SourceState.ACTIVE && fingerprint != null && fingerprint.equals(candidate);
        }
    }

    @Transactional
    int persist(SourceManifest.ManifestSource declaration, SourceLicense sourceLicense,
                Ingester.Harvest harvest, int generation) {
        Chunker chunker = new Chunker(new ChunkPolicy(targetChars, maxChars, minChars, ChunkPolicy.CURRENT_VERSION));
        int chunks = 0;
        for (DocumentDraft draft : harvest.documents()) {
            KnowledgeDocument document = new KnowledgeDocument();
            document.sourceId = declaration.id();
            document.generation = generation;
            document.externalId = draft.externalId();
            document.title = draft.title();
            document.partTitle = draft.partTitle();
            document.url = draft.url();
            document.authors = String.join(", ", draft.authors());
            document.licenseId = draft.licenseId();
            document.attribution = draft.attribution();
            document.checksum = Checksums.of(draft.html());
            String text = fr.lapetina.music.knowledge.text.HtmlText.toText(draft.html());
            document.body = text;
            document.wordCount = text.isBlank() ? 0 : text.split("\\s+").length;
            document.persist();

            List<HtmlSection> sections = SectionSplitter.split(draft.html());
            for (TextChunk piece : chunker.chunk(sections)) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.documentId = document.id;
                chunk.sourceId = declaration.id();
                chunk.generation = generation;
                chunk.chunkKey = Checksums.of(draft.externalId() + "|" + piece.sectionOrder()
                        + "|" + piece.chunkOrder() + "|" + piece.body());
                chunk.documentTitle = draft.title();
                chunk.sectionTitle = piece.sectionTitle();
                chunk.sectionOrder = piece.sectionOrder();
                chunk.chunkOrder = piece.chunkOrder();
                chunk.kind = piece.kind();
                chunk.body = piece.body();
                chunk.conceptIds = conceptTagger.tag(draft.title(), piece.sectionTitle(), piece.body());
                // The document's licence, not the source's: they are not always the same.
                chunk.licenseId = draft.licenseId();
                chunk.attribution = draft.attribution();
                chunk.url = draft.url();
                chunk.wordCount = piece.wordCount();
                chunk.persist();
                chunks++;
            }
        }
        return chunks;
    }

    /**
     * The chunks the next index generation should contain: everything already active from
     * other sources, plus what this run just wrote. Rebuilding the whole index keeps a
     * generation a complete, self-contained thing.
     */
    private List<KnowledgeChunk> allActiveAndPending(String sourceId, int generation) {
        List<KnowledgeChunk> chunks = new ArrayList<>(
                KnowledgeChunk.list("active = true and sourceId <> ?1", sourceId));
        chunks.addAll(KnowledgeChunk.list("sourceId = ?1 and generation = ?2", sourceId, generation));
        return chunks;
    }

    /** The switch-over. One transaction, so the index never serves two generations at once. */
    @Transactional
    void activate(String sourceId, int generation, int documents, int chunks,
                  String fingerprint, int parserVersion, String embeddingSignature) {
        KnowledgeChunk.update("active = false where sourceId = ?1", sourceId);
        KnowledgeDocument.update("active = false where sourceId = ?1", sourceId);
        KnowledgeChunk.update("active = true where sourceId = ?1 and generation = ?2", sourceId, generation);
        KnowledgeDocument.update("active = true where sourceId = ?1 and generation = ?2", sourceId, generation);
        // The superseded generation has served its purpose the moment this one is live.
        // Leaving it behind is not caution, it is a leak: every re-ingest would add another
        // full copy of the source and the database would grow without limit.
        KnowledgeChunk.delete("sourceId = ?1 and generation < ?2", sourceId, generation);
        KnowledgeDocument.delete("sourceId = ?1 and generation < ?2", sourceId, generation);

        KnowledgeSource source = KnowledgeSource.byId(sourceId);
        source.state = SourceState.ACTIVE;
        source.activeGeneration = generation;
        source.documentCount = documents;
        source.chunkCount = chunks;
        source.fingerprint = fingerprint;
        source.parserVersion = parserVersion;
        source.embeddingModel = embeddingSignature;
        source.retrievedAt = Instant.now();
        source.lastError = null;
        source.updatedAt = Instant.now();
    }

    @Transactional
    void recordState(String sourceId, SourceState state, String error) {
        KnowledgeSource source = KnowledgeSource.byId(sourceId);
        if (source == null) {
            return;
        }
        source.state = state;
        source.lastError = error;
        source.updatedAt = Instant.now();
    }

    @Transactional
    void recordFailure(String sourceId, String error) {
        recordState(sourceId, SourceState.FAILED, error);
    }

    Optional<Ingester> ingesterFor(String sourceId) {
        for (Ingester ingester : ingesters) {
            if (ingester.sourceId().equals(sourceId)) {
                return Optional.of(ingester);
            }
        }
        return Optional.empty();
    }
}
