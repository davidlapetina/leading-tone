package fr.lapetina.music.knowledge.router;

import fr.lapetina.music.knowledge.brief.PromptFence;
import fr.lapetina.music.knowledge.harmony.ExampleOrigin;
import fr.lapetina.music.knowledge.harmony.MusicalExample;

/**
 * Renders gathered knowledge into prompt text, keeping the three kinds visibly apart.
 *
 * <p>The order is the argument: computed facts, then verified examples, then quoted prose.
 * A model reads the certain things first and is told plainly that the quotation loses to
 * them on disagreement. Presenting all three as one undifferentiated context is what lets a
 * plausible sentence from a chapter override an arithmetic fact.
 */
public final class KnowledgeBlock {

    private KnowledgeBlock() {}

    public static String render(TutorKnowledge knowledge, int maxChars, int maxCharsPerChunk) {
        if (knowledge == null || knowledge.isEmpty()) {
            return "";
        }
        StringBuilder block = new StringBuilder();

        if (!knowledge.computed().isEmpty()) {
            block.append("\nThis application computed the following, and it is correct. State it as it is,\n")
                    .append("and do not recalculate it:\n");
            knowledge.computed().forEach(answer ->
                    block.append("- ").append(answer.statement()).append('\n'));
        }

        if (!knowledge.examples().isEmpty()) {
            block.append("\nExamples from annotated scores. These are real bars of real pieces. Cite them as\n")
                    .append("given, and never adjust a bar number or a work title to fit what you are saying:\n");
            for (MusicalExample example : knowledge.examples()) {
                block.append("- ").append(example.citation());
                if (example.romanNumeral() != null) {
                    block.append(" — ").append(example.romanNumeral());
                    if (example.globalKey() != null) {
                        block.append(" in ").append(example.globalKey());
                    }
                }
                if (example.origin() != ExampleOrigin.VERIFIED_CORPUS) {
                    block.append(" (written as an example, not from a score)");
                }
                block.append('\n');
            }
        }

        // The absence of evidence is itself evidence, and has to reach the model. Without
        // this line a model asked for a Beethoven example and given none will supply one.
        if (knowledge.corpusSearchedAndEmpty()) {
            block.append("\nNo verified example of this exists in the scores loaded here. Say so plainly.\n")
                    .append("Do not name a composer, a work, a movement or a bar number: there is no source\n")
                    .append("for one, and inventing it would be a fabrication the learner cannot check.\n");
        }

        if (!knowledge.retrieved().isEmpty()) {
            block.append('\n').append(
                    PromptFence.fence(knowledge.retrieved(), maxChars, maxCharsPerChunk));
        }
        return block.toString();
    }
}
