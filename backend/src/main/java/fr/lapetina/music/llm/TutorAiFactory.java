package fr.lapetina.music.llm;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import fr.lapetina.music.llm.tools.LearnerTools;
import fr.lapetina.music.llm.tools.TheoryTools;
import fr.lapetina.music.settings.Settings;
import fr.lapetina.music.settings.SettingsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import org.jboss.logging.Logger;

/**
 * Builds the tutor's language model from the persisted settings, and rebuilds it when they
 * change.
 *
 * <p>Configured through the extension, the model is fixed at injection and "use a smaller
 * one" means editing the environment and restarting. Built here, it is a decision the
 * learner can make while the application is running — which matters when the difference
 * between a usable tutor and an unusable one is which model fits in memory.
 */
@ApplicationScoped
public class TutorAiFactory {

    private static final Logger LOG = Logger.getLogger(TutorAiFactory.class);

    @Inject
    SettingsService settingsService;

    @Inject
    TheoryTools theoryTools;

    @Inject
    LearnerTools learnerTools;

    private volatile String builtFrom;
    private volatile Object service;

    /**
     * The tutor's AI service for the current settings, rebuilt only when something that
     * affects the model has actually changed.
     */
    public synchronized Object current(Settings settings) {
        String signature = settings.modelSignature();
        if (service != null && signature.equals(builtFrom)) {
            return service;
        }
        ChatModel model = OllamaChatModel.builder()
                .baseUrl(settings.baseUrl)
                .modelName(settings.model)
                .temperature(settings.temperature)
                .numCtx(settings.numCtx)
                .think(settings.think)
                .timeout(Duration.ofSeconds(settings.timeoutSeconds))
                .build();

        // Tool calling needs the model to see its own tool results, so a memory window is
        // required whenever tools are on.
        var memory = MessageWindowChatMemory.withMaxMessages(settings.memoryMessages);

        service = settings.toolsEnabled
                ? AiServices.builder(TutorAiService.class)
                        .chatModel(model)
                        .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(settings.memoryMessages))
                        .tools(theoryTools, learnerTools)
                        .build()
                : AiServices.builder(PlainTutorAiService.class)
                        .chatModel(model)
                        .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(settings.memoryMessages))
                        .build();
        builtFrom = signature;
        LOG.infof("Language model ready: %s at %s (tools %s, reasoning %s)",
                settings.model, settings.baseUrl,
                settings.toolsEnabled ? "on" : "off", settings.think ? "on" : "off");
        return service;
    }

    /** Forgets the built model, so the next turn constructs a fresh one. */
    public synchronized void invalidate() {
        service = null;
        builtFrom = null;
    }
}
