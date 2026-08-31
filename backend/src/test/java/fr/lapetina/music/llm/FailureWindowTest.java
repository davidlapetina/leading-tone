package fr.lapetina.music.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FailureWindowTest {

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void startsClosed() {
        assertFalse(new FailureWindow(Duration.ofMinutes(2)).isOpen(now));
    }

    @Test
    @DisplayName("after a failure the model is left alone, so turns stop paying the timeout")
    void opensAfterAFailure() {
        FailureWindow window = new FailureWindow(Duration.ofMinutes(2));
        window.recordFailure(now, "timed out");

        assertTrue(window.isOpen(now));
        assertTrue(window.isOpen(now.plusSeconds(119)));
        assertFalse(window.isOpen(now.plusSeconds(121)));
        assertEquals("timed out", window.lastFailure());
    }

    @Test
    void aSuccessClosesItImmediately() {
        FailureWindow window = new FailureWindow(Duration.ofMinutes(2));
        window.recordFailure(now, "timed out");
        window.recordSuccess();

        assertFalse(window.isOpen(now));
        assertEquals(Duration.ZERO, window.remaining(now));
    }

    @Test
    void reportsHowLongIsLeft() {
        FailureWindow window = new FailureWindow(Duration.ofMinutes(2));
        window.recordFailure(now, "timed out");
        assertEquals(90, window.remaining(now.plusSeconds(30)).toSeconds());
    }
}
