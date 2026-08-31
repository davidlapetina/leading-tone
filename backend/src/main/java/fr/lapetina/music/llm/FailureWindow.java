package fr.lapetina.music.llm;

import java.time.Duration;
import java.time.Instant;

/**
 * Stops the tutor paying for a model that is not answering.
 *
 * <p>An unreachable or overloaded local model does not fail fast: the client waits out
 * its timeout and then retries it, so a single turn can block for minutes before the
 * fallback runs. Doing that on every turn makes the application unusable. After a
 * failure, the model is left alone for a while and turns are produced from templates
 * immediately instead.
 */
public class FailureWindow {

    private final Duration cooldown;
    private volatile Instant closedUntil = Instant.EPOCH;
    private volatile String lastFailure;

    public FailureWindow(Duration cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isOpen(Instant now) {
        return now.isBefore(closedUntil);
    }

    public void recordFailure(Instant now, String reason) {
        closedUntil = now.plus(cooldown);
        lastFailure = reason;
    }

    public void recordSuccess() {
        closedUntil = Instant.EPOCH;
        lastFailure = null;
    }

    public String lastFailure() {
        return lastFailure;
    }

    public Duration remaining(Instant now) {
        return isOpen(now) ? Duration.between(now, closedUntil) : Duration.ZERO;
    }
}
