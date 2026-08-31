package fr.lapetina.music.learner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression. Two requests arriving together used to create two learners, after which the
 * interface read from one and wrote to the other: evidence went in and the panel stayed
 * empty. A React effect running twice on mount was enough to trigger it.
 */
@QuarkusTest
class LearnerIdentityTest {

    @Inject
    LearnerService learnerService;

    @Test
    @DisplayName("concurrent requests all get the same learner")
    void neverCreatesASecondLearner() throws Exception {
        learnerService.reset();

        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Callable<UUID>> calls = java.util.Collections.nCopies(16,
                    () -> learnerService.current().id);
            List<Future<UUID>> results = pool.invokeAll(calls);

            Set<UUID> ids = results.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).collect(Collectors.toSet());

            assertEquals(1, ids.size(), "several learners were created: " + ids);
            assertEquals(Learner.SINGLETON_ID, ids.iterator().next());
        }
        assertEquals(1, Learner.count());
    }

    @Test
    void resetLeavesExactlyOneEmptyLearner() {
        Learner learner = learnerService.current();
        learnerService.reset();

        assertEquals(1, Learner.count());
        assertEquals(learner.id, learnerService.current().id);
        assertTrue(learnerService.snapshot(learnerService.current()).isBlank());
    }
}
