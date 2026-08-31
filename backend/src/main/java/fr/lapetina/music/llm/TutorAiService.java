package fr.lapetina.music.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import java.util.UUID;

/**
 * The tutor's voice, with access to the theory engine as tools.
 *
 * <p>Note what is absent from its permissions. The model does not choose the concept, the
 * action, the difficulty or the verdict on an answer, and it has no way to write mastery
 * — the learner tools offer a proposal, not a setter.
 *
 * <p>Tool calling needs a model that actually supports it; smaller local models tend to
 * type the call out as text instead. Set {@code music.llm.tools-enabled=false} for those
 * and {@link PlainTutorAiService} is used instead.
 *
 * <p>Conversation memory is keyed by session and lives in process; the durable record of
 * what was said is the {@code interaction} table, not this window.
 */
public interface TutorAiService {

    @SystemMessage(TutorPrompts.SYSTEM_WITH_TOOLS)
    @UserMessage(TutorPrompts.USER)
    String teach(@MemoryId UUID sessionId, String learnerState, String instruction, String exerciseBlock,
                 String learnerMessage);
}
