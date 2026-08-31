package fr.lapetina.music.settings;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * How this installation is configured, kept in the database rather than the environment.
 *
 * <p>Environment variables cannot be changed from inside the application, which makes
 * "switch to a smaller model" a restart rather than a decision. These are editable while
 * it runs, and the language model is rebuilt when they change.
 */
@Entity
@Table(name = "settings")
public class Settings extends PanacheEntityBase {

    /** One installation, one row. */
    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-00005e771465");

    @Id
    public UUID id = SINGLETON_ID;

    @Column(name = "llm_enabled", nullable = false)
    public boolean llmEnabled = true;

    @Column(name = "tools_enabled", nullable = false)
    public boolean toolsEnabled = true;

    @Column(nullable = false)
    public String model = "qwen3:8b";

    @Column(name = "base_url", nullable = false)
    public String baseUrl = "http://localhost:11434";

    @Column(nullable = false)
    public double temperature = 0.8;

    @Column(name = "num_ctx", nullable = false)
    public int numCtx = 8192;

    /** Reasoning models think before answering. Measured here: three times slower, and worse. */
    @Column(nullable = false)
    public boolean think = false;

    @Column(name = "timeout_seconds", nullable = false)
    public int timeoutSeconds = 60;

    @Column(name = "cooldown_seconds", nullable = false)
    public int cooldownSeconds = 120;

    @Column(name = "memory_messages", nullable = false)
    public int memoryMessages = 10;

    @Column(name = "learner_name", nullable = false)
    public String learnerName = "Student";

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    /** Changes that require the language model to be rebuilt rather than merely re-read. */
    public String modelSignature() {
        return "%s|%s|%.3f|%d|%s|%d|%s|%d".formatted(model, baseUrl, temperature, numCtx, think,
                timeoutSeconds, toolsEnabled, memoryMessages);
    }
}
