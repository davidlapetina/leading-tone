package fr.lapetina.music.tutor;

import fr.lapetina.music.knowledge.router.KnowledgeBlock;
import fr.lapetina.music.knowledge.router.KnowledgeRouter;
import fr.lapetina.music.knowledge.router.TheoryAnswer;
import fr.lapetina.music.knowledge.router.TutorKnowledge;
import fr.lapetina.music.llm.QuestionAnswerer;
import fr.lapetina.music.settings.SettingsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Answers a question asked directly, rather than one the teaching policy chose to raise.
 *
 * <p>The same three kinds of knowledge as a teaching turn — computed, quoted, and real bars —
 * gathered the same way and kept as separate as they are there. What differs is that the
 * question is the whole request, so retrieval always runs, and the answer is returned with
 * the material it was built from so a reader can check it rather than trust it.
 */
@ApplicationScoped
public class AskService {

    @Inject
    KnowledgeRouter router;

    @Inject
    QuestionAnswerer answerer;

    @Inject
    SettingsService settingsService;

    @ConfigProperty(name = "music.knowledge.prompt.max-chars", defaultValue = "2600")
    int maxChars;

    @ConfigProperty(name = "music.knowledge.prompt.max-chars-per-chunk", defaultValue = "900")
    int maxCharsPerChunk;

    /**
     * @param conversationId groups follow-up questions, so "and in F?" still means something.
     *                       A new id starts a fresh conversation.
     */
    public Answer ask(String question, UUID conversationId) {
        TutorKnowledge knowledge = router.forQuestion(question);
        String material = KnowledgeBlock.render(knowledge, maxChars, maxCharsPerChunk);
        String prose = answerer
                .answer(conversationId == null ? UUID.randomUUID() : conversationId,
                        question, material, settingsService.current())
                .orElseGet(() -> withoutAModel(knowledge));
        return new Answer(question, prose, knowledge, !settingsService.current().llmEnabled);
    }

    /**
     * The answer when there is no model, written from what was found.
     *
     * <p>Plainer than a model would write it, and every word of it is either computed or
     * quoted. The application is meant to be usable with the model switched off, and a
     * question box that only works when Ollama is running would not be.
     */
    private static String withoutAModel(TutorKnowledge knowledge) {
        StringBuilder answer = new StringBuilder();
        for (TheoryAnswer computed : knowledge.computed()) {
            answer.append(computed.statement()).append('\n');
        }
        if (!knowledge.retrieved().isEmpty()) {
            if (answer.length() > 0) {
                answer.append('\n');
            }
            answer.append("From the sources below:\n\n")
                    .append(knowledge.retrieved().get(0).body().strip());
        }
        if (!knowledge.examples().isEmpty()) {
            answer.append("\n\nReal examples are shown below, from annotated scores.");
        }
        if (knowledge.corpusSearchedAndEmpty()) {
            answer.append("\n\nNo verified example of that was found in the corpora.");
        }
        if (answer.length() == 0) {
            return "Nothing was found for that. Try naming a chord, a scale, an interval or a "
                    + "Roman numeral, or bring more sources in from Settings.";
        }
        return answer.toString().strip();
    }

    /**
     * @param withoutAModel whether this was written from the material rather than by a model,
     *                      which the interface says out loud rather than passing it off
     */
    public record Answer(String question, String answer, TutorKnowledge knowledge, boolean withoutAModel) {}
}
