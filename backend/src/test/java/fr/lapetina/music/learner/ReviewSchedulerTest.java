package fr.lapetina.music.learner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewSchedulerTest {

    private final ReviewScheduler scheduler = new ReviewScheduler();
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    private LearnerConcept withMastery(double mastery) {
        LearnerConcept concept = new LearnerConcept();
        concept.conceptId = "chord-inversion";
        concept.mastery = mastery;
        return concept;
    }

    @Test
    @DisplayName("something still being learned is not put on a review schedule")
    void doesNotScheduleWhatIsNotYetHeld() {
        LearnerConcept concept = withMastery(0.2);
        scheduler.schedule(concept, true, now);
        assertNull(concept.nextReviewAt);
        assertEquals(0, concept.reviewIntervalDays);
    }

    @Test
    void intervalsExpandOnSuccess() {
        LearnerConcept concept = withMastery(0.5);
        scheduler.schedule(concept, true, now);
        assertEquals(1, concept.reviewIntervalDays);
        assertEquals(now.plus(Duration.ofDays(1)), concept.nextReviewAt);

        scheduler.schedule(concept, true, now);
        assertEquals(2, concept.reviewIntervalDays);

        scheduler.schedule(concept, true, now);
        assertEquals(4, concept.reviewIntervalDays);
    }

    @Test
    void strongerMasteryEarnsALongerGap() {
        LearnerConcept weak = withMastery(0.5);
        LearnerConcept strong = withMastery(1.0);
        weak.reviewIntervalDays = 10;
        strong.reviewIntervalDays = 10;
        scheduler.schedule(weak, true, now);
        scheduler.schedule(strong, true, now);
        assertTrue(strong.reviewIntervalDays > weak.reviewIntervalDays);
        assertEquals(2.5, scheduler.easeFor(1.0), 1e-9);
        assertEquals(1.5, scheduler.easeFor(0.0), 1e-9);
    }

    @Test
    void aMistakeSendsItBackToTomorrow() {
        LearnerConcept concept = withMastery(0.8);
        concept.reviewIntervalDays = 30;
        scheduler.schedule(concept, false, now);
        assertEquals(1, concept.reviewIntervalDays);
    }

    @Test
    void intervalsAreCapped() {
        LearnerConcept concept = withMastery(1.0);
        concept.reviewIntervalDays = 170;
        scheduler.schedule(concept, true, now);
        assertEquals(ReviewScheduler.MAX_INTERVAL_DAYS, concept.reviewIntervalDays);
    }
}
