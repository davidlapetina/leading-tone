package fr.lapetina.music.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Configuration lives in the database so it can be changed from inside the application.
 * These cover the parts that must hold for that to be safe.
 */
@QuarkusTest
class SettingsServiceTest {

    @Inject
    SettingsService settingsService;

    private static SettingsUpdate only(String model, Double temperature) {
        return new SettingsUpdate(null, null, null, null, model, null, temperature, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("there is one settings row, seeded with the values the application ships with")
    void seedsFromTheShippedDefaults() {
        settingsService.reset();
        Settings settings = settingsService.current();

        assertEquals(Settings.SINGLETON_ID, settings.id);
        assertEquals("qwen3:8b", settings.model);
        assertEquals("http://localhost:11434", settings.baseUrl);
        assertEquals(8192, settings.numCtx);
        assertEquals(false, settings.think);
        assertEquals(1, Settings.count());
    }

    @Test
    void changesArePartial() {
        settingsService.reset();
        double before = settingsService.current().temperature;

        settingsService.update(only("qwen3:4b", null));
        assertEquals("qwen3:4b", settingsService.current().model);
        assertEquals(before, settingsService.current().temperature, "untouched fields should not move");
    }

    @Test
    @DisplayName("values that would break the model are clamped rather than accepted")
    void clampsNonsense() {
        settingsService.reset();
        settingsService.update(only(null, 40.0));
        assertTrue(settingsService.current().temperature <= 2.0);

        settingsService.update(new SettingsUpdate(null, null, null, null, null, null, null, 10, null, 0, -5, 0, null));
        Settings settings = settingsService.current();
        assertTrue(settings.numCtx >= 1024);
        assertTrue(settings.timeoutSeconds >= 5);
        assertTrue(settings.cooldownSeconds >= 0);
        assertTrue(settings.memoryMessages >= 2);
    }

    @Test
    @DisplayName("a change that affects the model is visible in its signature, so it gets rebuilt")
    void signatureTracksWhatMatters() {
        settingsService.reset();
        String before = settingsService.current().modelSignature();

        settingsService.update(new SettingsUpdate(null, null, null, null, null, null, null, null, null, null, null, null, "David"));
        assertEquals(before, settingsService.current().modelSignature(), "a name change is not a model change");

        settingsService.update(only("qwen3:30b-a3b", null));
        assertNotEquals(before, settingsService.current().modelSignature());
    }

    @Test
    void blankValuesAreIgnoredRatherThanStored() {
        settingsService.reset();
        settingsService.update(only("   ", null));
        assertEquals("qwen3:8b", settingsService.current().model);
    }
}
