package fr.lapetina.music.learner;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.transaction.Transactional;

/**
 * Puts the learner in place before any request can ask for one, so the request path only
 * ever reads. Creating a learner lazily on first use is what allowed several to be
 * created at once.
 */
@ApplicationScoped
public class LearnerSeeder {

    @Inject
    LearnerService learnerService;

    @Transactional
    void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION + 200) StartupEvent event) {
        learnerService.current();
    }
}
