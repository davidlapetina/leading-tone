package fr.lapetina.music.llm;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
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
    Instance<TutorAiService> withTools;

    @Inject
    Instance<PlainTutorAiService> withoutTools;

    @Inject
    TutorPromptBuilder promptBuilder;

    @ConfigProperty(name = "music.llm.enabled", defaultValue = "true")
    boolean llmEnabled;

    /**
     * Off by default: the default model is a small local one, and small models tend to
     * type tool calls out as text rather than call them. Turn it on for a model that
     * handles tools properly.
     */
    @ConfigProperty(name = "music.llm.tools-enabled", defaultValue = "false")
    boolean toolsEnabled;

    @ConfigProperty(name = "quarkus.langchain4j.ollama.chat-model.model-id", defaultValue = "unknown")
    String modelId;

    /**
     * How long to stop calling the model after it fails. Local models fail slowly, and
     * paying that on every turn is worse than the templates.
     */
    @ConfigProperty(name = "music.llm.failure-cooldown", defaultValue = "PT2M")
    Duration failureCooldown;

    private FailureWindow failures;

    @PostConstruct
    void init() {
        failures = new FailureWindow(failureCooldown);
    }

    @Override
    public String respond(TutorRequest request) {
        Instant now = Instant.now();
        if (!llmEnabled || !available() || failures.isOpen(now)) {
            return fallback.respond(request);
        }
        try {
            String answer = call(request);
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
            LOG.warnf("Language model unavailable; using templates for the next %s: %s",
                    failureCooldown, modelFailure.getMessage());
            return fallback.respond(request);
        }
    }

    private String call(TutorRequest request) {
        String learnerState = promptBuilder.learnerState(request.snapshot());
        String instruction = promptBuilder.instruction(request.decision());
        String exerciseBlock = exerciseBlock(request);
        String learnerMessage = learnerMessage(request);

        if (toolsEnabled && withTools.isResolvable()) {
            return withTools.get().teach(request.sessionId(), learnerState, instruction, exerciseBlock,
                    learnerMessage);
        }
        return withoutTools.get().teach(request.sessionId(), learnerState, instruction, exerciseBlock,
                learnerMessage);
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

    private boolean available() {
        return toolsEnabled ? withTools.isResolvable() : withoutTools.isResolvable();
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
        if (request.evaluationFeedback() != null && !request.evaluationFeedback().isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("The evaluator's verdict, which is final and which you must not contradict: ")
                    .append(request.evaluationFeedback());
        }
        return builder.isEmpty() ? "(nothing yet)" : builder.toString();
    }

    @Override
    public boolean isAvailable() {
        return llmEnabled && available() && !failures.isOpen(Instant.now());
    }

    /** Which model is actually configured, so the interface can say so. */
    public String modelId() {
        return modelId;
    }

    public boolean toolsEnabled() {
        return toolsEnabled;
    }

    @Override
    public String describe() {
        if (!llmEnabled) {
            return "template (language model disabled)";
        }
        Instant now = Instant.now();
        if (failures.isOpen(now)) {
            return "template (model unreachable, retrying in %ds: %s)"
                    .formatted(failures.remaining(now).toSeconds(), failures.lastFailure());
        }
        return toolsEnabled ? "language model with theory tools" : "language model";
    }
}
