package fr.lapetina.music.settings;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Reads and writes the one settings row, seeding it from the values the application ships
 * with. Everything else asks this rather than the environment.
 */
@ApplicationScoped
public class SettingsService {

    @ConfigProperty(name = "music.defaults.model", defaultValue = "qwen3:8b")
    String defaultModel;

    @ConfigProperty(name = "music.defaults.base-url", defaultValue = "http://localhost:11434")
    String defaultBaseUrl;

    @ConfigProperty(name = "music.defaults.llm-enabled", defaultValue = "true")
    boolean defaultLlmEnabled;

    @Transactional
    void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION + 50) StartupEvent event) {
        current();
    }

    /** The settings, seeded on first use from the shipped defaults. */
    @Transactional
    public Settings current() {
        Settings existing = Settings.findById(Settings.SINGLETON_ID);
        if (existing != null) {
            return existing;
        }
        Settings created = new Settings();
        created.model = defaultModel;
        created.baseUrl = defaultBaseUrl;
        created.llmEnabled = defaultLlmEnabled;
        created.persist();
        return created;
    }

    @Transactional
    public Settings update(SettingsUpdate update) {
        Settings settings = current();
        if (update.llmEnabled() != null) {
            settings.llmEnabled = update.llmEnabled();
        }
        if (update.toolsEnabled() != null) {
            settings.toolsEnabled = update.toolsEnabled();
        }
        if (update.model() != null && !update.model().isBlank()) {
            settings.model = update.model().trim();
        }
        if (update.baseUrl() != null && !update.baseUrl().isBlank()) {
            settings.baseUrl = update.baseUrl().trim();
        }
        if (update.temperature() != null) {
            settings.temperature = clamp(update.temperature(), 0.0, 2.0);
        }
        if (update.numCtx() != null) {
            settings.numCtx = (int) clamp(update.numCtx(), 1024, 131_072);
        }
        if (update.think() != null) {
            settings.think = update.think();
        }
        if (update.timeoutSeconds() != null) {
            settings.timeoutSeconds = (int) clamp(update.timeoutSeconds(), 5, 900);
        }
        if (update.cooldownSeconds() != null) {
            settings.cooldownSeconds = (int) clamp(update.cooldownSeconds(), 0, 3600);
        }
        if (update.memoryMessages() != null) {
            settings.memoryMessages = (int) clamp(update.memoryMessages(), 2, 60);
        }
        if (update.learnerName() != null && !update.learnerName().isBlank()) {
            settings.learnerName = update.learnerName().trim();
        }
        settings.updatedAt = Instant.now();
        return settings;
    }

    /** Puts everything back to what the application ships with. */
    @Transactional
    public Settings reset() {
        Settings settings = current();
        Settings fresh = new Settings();
        settings.llmEnabled = defaultLlmEnabled;
        settings.toolsEnabled = fresh.toolsEnabled;
        settings.model = defaultModel;
        settings.baseUrl = defaultBaseUrl;
        settings.temperature = fresh.temperature;
        settings.numCtx = fresh.numCtx;
        settings.think = fresh.think;
        settings.timeoutSeconds = fresh.timeoutSeconds;
        settings.cooldownSeconds = fresh.cooldownSeconds;
        settings.memoryMessages = fresh.memoryMessages;
        settings.learnerName = fresh.learnerName;
        settings.updatedAt = Instant.now();
        return settings;
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
