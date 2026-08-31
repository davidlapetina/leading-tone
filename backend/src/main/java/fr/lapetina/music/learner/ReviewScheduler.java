package fr.lapetina.music.learner;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Expanding-interval review. Stronger mastery earns a longer gap; a mistake sends the
 * concept back to tomorrow.
 */
@ApplicationScoped
public class ReviewScheduler {

    static final int FIRST_INTERVAL_DAYS = 1;
    static final int MAX_INTERVAL_DAYS = 180;
    static final double MIN_EASE = 1.5;
    static final double MAX_EASE = 2.5;

    /** Only concepts the learner actually has a grip on are worth scheduling. */
    static final double SCHEDULING_THRESHOLD = MasteryService.LEARNING_THRESHOLD;

    public void schedule(LearnerConcept target, boolean success, Instant now) {
        if (target.mastery < SCHEDULING_THRESHOLD) {
            // Still being learned rather than retained: the policy will pick it up on merit.
            target.reviewIntervalDays = 0;
            target.nextReviewAt = null;
            return;
        }
        int interval;
        if (!success) {
            interval = FIRST_INTERVAL_DAYS;
        } else if (target.reviewIntervalDays <= 0) {
            interval = FIRST_INTERVAL_DAYS;
        } else {
            interval = (int) Math.round(target.reviewIntervalDays * easeFor(target.mastery));
        }
        target.reviewIntervalDays = Math.min(interval, MAX_INTERVAL_DAYS);
        target.nextReviewAt = now.plus(Duration.ofDays(target.reviewIntervalDays));
    }

    /** 1.5 at the bottom of the retention range, 2.5 at full mastery. */
    public double easeFor(double mastery) {
        return MIN_EASE + (MAX_EASE - MIN_EASE) * Math.max(0.0, Math.min(1.0, mastery));
    }

    public List<LearnerConcept> due(Learner learner, Instant now) {
        return LearnerConcept.find(
                "learner = ?1 and nextReviewAt is not null and nextReviewAt <= ?2 order by nextReviewAt",
                learner, now).list();
    }

    public boolean hasDue(Learner learner, Instant now) {
        return LearnerConcept.count(
                "learner = ?1 and nextReviewAt is not null and nextReviewAt <= ?2", learner, now) > 0;
    }
}
