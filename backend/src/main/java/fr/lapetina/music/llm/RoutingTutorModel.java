package fr.lapetina.music.llm;

import fr.lapetina.music.settings.Settings;
import fr.lapetina.music.settings.SettingsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Sends the turn to a language model, and falls back to templates when that is not
 * possible or when what comes back is not prose.
 *
 * <p>A tutor that stops working because Ollama is not running, or that reads out a JSON
 * blob because a small model mishandled a tool schema, would be a bad tutor. The parts
 * that matter — the learner model, the policy, the evaluators — do not depend on a model
 * at all, so a bad turn degrades into a plain one rather than into nonsense.
 */
@ApplicationScoped
public class RoutingTutorModel implements TutorModel {

    private static final Logger LOG = Logger.getLogger(RoutingTutorModel.class);

    private final TemplateTutor fallback = new TemplateTutor();

    @Inject
    TutorAiFactory factory;

    @Inject
    SettingsService settingsService;

    @Inject
    TutorPromptBuilder promptBuilder;

    private volatile FailureWindow failures = new FailureWindow(Duration.ofMinutes(2));
    private volatile long cooldownSeconds = 120;

    @Override
    public String respond(TutorRequest request) {
        Settings settings = settingsService.current();
        useCooldown(settings.cooldownSeconds);
        Instant now = Instant.now();
        if (!settings.llmEnabled || failures.isOpen(now)) {
            return fallback.respond(request);
        }
        try {
            String answer = call(request, settings);
            failures.recordSuccess();

            if (answer == null || answer.isBlank()) {
                LOG.warn("The model returned nothing; using the template turn instead.");
                return fallback.respond(request);
            }
            String trimmed = answer.trim();
            if (looksLikeMarkupRatherThanTeaching(trimmed)) {
                LOG.warnf("The model replied with markup rather than prose (%s); using the template turn instead.",
                        trimmed.length() > 120 ? trimmed.substring(0, 120) + "…" : trimmed);
                return fallback.respond(request);
            }
            return trimmed;
        } catch (RuntimeException modelFailure) {
            failures.recordFailure(Instant.now(), modelFailure.getMessage());
            LOG.warnf("Language model unavailable; using templates for the next %ds: %s",
                    cooldownSeconds, modelFailure.getMessage());
            return fallback.respond(request);
        }
    }

    private String call(TutorRequest request, Settings settings) {
        String learnerState = promptBuilder.learnerState(request.snapshot());
        String instruction = promptBuilder.instruction(request.decision(), request.knowledge(), request.snapshot());
        String exerciseBlock = exerciseBlock(request);
        String learnerMessage = learnerMessage(request);

        Object service = factory.current(settings);
        if (service instanceof TutorAiService withTools) {
            return withTools.teach(request.sessionId(), learnerState, instruction, exerciseBlock, learnerMessage);
        }
        return ((PlainTutorAiService) service)
                .teach(request.sessionId(), learnerState, instruction, exerciseBlock, learnerMessage);
    }

    /** The cooldown is a setting, so the window follows it without a restart. */
    private void useCooldown(int seconds) {
        if (seconds != cooldownSeconds) {
            cooldownSeconds = seconds;
            failures = new FailureWindow(Duration.ofSeconds(seconds));
        }
    }

    /**
     * A teacher's turn is prose. A reply that opens as JSON or a tag is a model failing at
     * tool calling, and reading it aloud to the learner would be worse than saying nothing.
     */
    static boolean looksLikeMarkupRatherThanTeaching(String answer) {
        char first = answer.charAt(0);
        if (first == '{' || first == '[') {
            return true;
        }
        if (answer.startsWith("<tool") || answer.startsWith("<function")) {
            return true;
        }
        return answer.contains("\"parameters\":") || answer.contains("\"arguments\":");
    }

    private String exerciseBlock(TutorRequest request) {
        if (request.exercise() == null) {
            return promptBuilder.exerciseBlock(null, null);
        }
        return promptBuilder.exerciseBlock(request.exercise().prompt, request.exercise().answerMode.name(),
                request.exercise().taskKind.describe());
    }

    private static String learnerMessage(TutorRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.learnerMessage() != null && !request.learnerMessage().isBlank()) {
            builder.append(request.learnerMessage());
        }
        if (request.modelDirective() != null && !request.modelDirective().isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(request.modelDirective());
        }
        return builder.isEmpty() ? "(nothing yet)" : builder.toString();
    }

    @Override
    public boolean isAvailable() {
        return settingsService.current().llmEnabled && !failures.isOpen(Instant.now());
    }

    @Override
    public String describe() {
        Settings settings = settingsService.current();
        if (!settings.llmEnabled) {
            return "template (language model disabled)";
        }
        Instant now = Instant.now();
        if (failures.isOpen(now)) {
            return "template (model unreachable, retrying in %ds: %s)"
                    .formatted(failures.remaining(now).toSeconds(), failures.lastFailure());
        }
        return settings.toolsEnabled ? "language model with theory tools" : "language model";
    }
}
