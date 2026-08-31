package fr.lapetina.music.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/**
 * The same tutor, with no tools.
 *
 * <p>A small local model handed a tool schema will often write the call out as JSON in
 * its reply instead of calling anything, which produces a teacher that speaks in braces.
 * Everything the tutor strictly needs — the concept, the action, the exercise and its
 * answer — is computed in Java and handed over in the prompt, so removing tools costs
 * accuracy nowhere; it only removes the model's ability to look things up mid-sentence.
 */
@ApplicationScoped
@RegisterAiService
public interface PlainTutorAiService {

    @SystemMessage(TutorPrompts.SYSTEM)
    @UserMessage(TutorPrompts.USER)
    String teach(@MemoryId UUID sessionId, String learnerState, String instruction, String exerciseBlock,
                 String learnerMessage);
}
