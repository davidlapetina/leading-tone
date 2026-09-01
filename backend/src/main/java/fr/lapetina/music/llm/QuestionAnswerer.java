package fr.lapetina.music.llm;

import fr.lapetina.music.settings.Settings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Puts a question to the language model, grounded in material gathered beforehand.
 *
 * <p>Returns nothing rather than something invented: with no model, an unreachable one, or a
 * reply that is not prose, the caller writes the answer from the material itself. A question
 * answered from published sources and computed facts is still answered.
 */
@ApplicationScoped
public class QuestionAnswerer {

    private static final Logger LOG = Logger.getLogger(QuestionAnswerer.class);

    @Inject
    TutorAiFactory factory;

    /** The model's answer, or empty when there is no model or it did not produce prose. */
    public Optional<String> answer(UUID conversationId, String question, String material, Settings settings) {
        if (!settings.llmEnabled) {
            return Optional.empty();
        }
        try {
            Object service = factory.current(settings);
            String reply = service instanceof TutorAiService withTools
                    ? withTools.answer(conversationId, question, material)
                    : ((PlainTutorAiService) service).answer(conversationId, question, material);
            if (reply == null || reply.isBlank()
                    || RoutingTutorModel.looksLikeMarkupRatherThanTeaching(reply.trim())) {
                return Optional.empty();
            }
            return Optional.of(reply.trim());
        } catch (RuntimeException modelFailure) {
            LOG.warnf("The model could not answer; answering from the material instead: %s",
                    modelFailure.getMessage());
            return Optional.empty();
        }
    }
}
