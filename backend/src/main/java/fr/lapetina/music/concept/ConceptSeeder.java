package fr.lapetina.music.concept;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Mirrors {@code concepts.json} into the database so the graph can be joined against
 * learner data in SQL. The JSON file stays the source of truth; these rows are rebuilt
 * on every start.
 */
@ApplicationScoped
public class ConceptSeeder {

    private static final Logger LOG = Logger.getLogger(ConceptSeeder.class);

    @Inject
    ConceptGraph conceptGraph;

    @Inject
    EntityManager entityManager;

    @Transactional
    void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION + 100) StartupEvent event) {
        seed();
    }

    @Transactional
    public void seed() {
        entityManager.createNativeQuery("delete from concept_prerequisite").executeUpdate();
        entityManager.createNativeQuery("delete from concept").executeUpdate();
        for (Concept concept : conceptGraph.all()) {
            entityManager.createNativeQuery(
                            "insert into concept (id, name, description, category, intrinsic_difficulty) "
                                    + "values (?1, ?2, ?3, ?4, ?5)")
                    .setParameter(1, concept.id())
                    .setParameter(2, concept.name())
                    .setParameter(3, concept.description())
                    .setParameter(4, concept.category().name())
                    .setParameter(5, concept.intrinsicDifficulty())
                    .executeUpdate();
        }
        for (Concept concept : conceptGraph.all()) {
            for (String prerequisite : concept.prerequisites()) {
                entityManager.createNativeQuery(
                                "insert into concept_prerequisite (concept_id, prerequisite_id) values (?1, ?2)")
                        .setParameter(1, concept.id())
                        .setParameter(2, prerequisite)
                        .executeUpdate();
            }
        }
        LOG.infof("Seeded %d concepts", conceptGraph.size());
    }
}
