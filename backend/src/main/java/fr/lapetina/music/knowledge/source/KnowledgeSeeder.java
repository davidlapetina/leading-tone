package fr.lapetina.music.knowledge.source;

import fr.lapetina.music.knowledge.index.KnowledgeIndex;
import fr.lapetina.music.knowledge.license.SourceLicense;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Registers the declared sources at startup, and opens whatever index is already on disk.
 *
 * <p>Deliberately does not download anything. Reading the internet on every start would
 * make the application slow, fragile and rude to the publishers it depends on; ingestion
 * is something a person asks for.
 */
@ApplicationScoped
public class KnowledgeSeeder {

    private static final Logger LOG = Logger.getLogger(KnowledgeSeeder.class);

    @Inject
    SourceRegistry registry;

    @Inject
    KnowledgeIndex index;

    @ConfigProperty(name = "music.defaults.knowledge-enabled", defaultValue = "true")
    boolean knowledgeEnabled;

    /** After the concept seeder, whose graph the concept tagger reads. */
    void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION + 150) StartupEvent event) {
        seed();
        if (knowledgeEnabled) {
            index.openCurrent();
        }
    }

    @Transactional
    public void seed() {
        int created = 0;
        for (SourceManifest.ManifestSource declared : registry.all()) {
            KnowledgeSource stored = KnowledgeSource.byId(declared.id());
            SourceLicense license = registry.licenseOf(declared);
            if (stored == null) {
                stored = new KnowledgeSource();
                stored.id = declared.id();
                stored.state = SourceState.DISCOVERED;
                created++;
            }
            // The declaration wins on every start: what a source is, and on whose terms,
            // is decided in the manifest and never drifts because of something we stored.
            stored.displayName = declared.name();
            stored.licenseId = license.id();
            stored.licenseStatus = license.status();
            stored.ingestionMode = declared.ingestionMode();
            stored.enabled = declared.isEnabled();
            stored.sourceVersion = declared.version();
            stored.updatedAt = Instant.now();
            stored.persist();
        }
        if (created > 0) {
            LOG.infof("Registered %d knowledge sources from %s", created, SourceRegistry.MANIFEST);
        }
    }
}
