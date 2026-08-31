package fr.lapetina.music.knowledge.text;

import fr.lapetina.music.knowledge.chunk.ChunkKind;
import fr.lapetina.music.knowledge.index.MusicSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cuts a chapter into passages along the shape of the writing.
 *
 * <p>The rules, in order of precedence:
 *
 * <ol>
 *   <li><strong>A passage never crosses a heading.</strong> That is what makes the section
 *       title on a retrieved passage true rather than approximate.
 *   <li><strong>A definition keeps its example.</strong> If a paragraph is followed by one
 *       that opens like an example, or by a line that is mostly chord symbols, the two
 *       stay together even if that overshoots the target length.
 *   <li><strong>"Key Takeaways" blocks are never split.</strong> In this textbook they are
 *       the densest and most quotable text on the page.
 *   <li>Only then, break at a paragraph boundary near the target length. Never mid-sentence.
 * </ol>
 *
 * <p>A fixed token count would be simpler and would cut a definition away from the example
 * that explains it, which is exactly the passage a learner needs.
 */
public final class Chunker {

    private static final List<String> EXAMPLE_LEADS = List.of(
            "for example", "for instance", "consider", "example", "notice", "compare",
            "in the example", "play ", "listen", "[figure:");

    private static final String TAKEAWAY = "key takeaways";

    private final ChunkPolicy policy;

    public Chunker(ChunkPolicy policy) {
        this.policy = policy;
    }

    public List<TextChunk> chunk(List<HtmlSection> sections) {
        List<TextChunk> chunks = new ArrayList<>();
        for (HtmlSection section : sections) {
            chunks.addAll(chunkSection(section));
        }
        return merge(chunks);
    }

    private List<TextChunk> chunkSection(HtmlSection section) {
        List<TextChunk> chunks = new ArrayList<>();
        if (isTakeaway(section)) {
            // Never split: the whole point of the block is that it is complete.
            chunks.add(new TextChunk(section.heading(), section.order(), 0,
                    ChunkKind.TAKEAWAY, withHeading(section, section.text())));
            return chunks;
        }
        List<String> paragraphs = paragraphs(section.text());
        StringBuilder current = new StringBuilder();
        int order = 0;
        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);
            boolean glueNext = i + 1 < paragraphs.size() && isExample(paragraphs.get(i + 1));
            append(current, paragraph);

            boolean longEnough = current.length() >= policy.targetChars();
            boolean tooLong = current.length() >= policy.maxChars();
            if ((longEnough && !glueNext) || tooLong) {
                chunks.add(build(section, order++, current.toString()));
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(build(section, order, current.toString()));
        }
        return chunks;
    }

    private TextChunk build(HtmlSection section, int order, String body) {
        String text = order == 0 ? withHeading(section, body) : body;
        return new TextChunk(section.heading(), section.order(), order, kindOf(section, order, body), text);
    }

    /**
     * The heading is repeated into the first passage of a section. A passage is read on its
     * own by both the ranker and the model, and "Secondary Dominants" at the top of it is
     * the cheapest possible context.
     */
    private String withHeading(HtmlSection section, String body) {
        return section.isIntroduction() ? body : section.heading() + "\n\n" + body;
    }

    private ChunkKind kindOf(HtmlSection section, int order, String body) {
        if (isTakeaway(section)) {
            return ChunkKind.TAKEAWAY;
        }
        if (isExample(body)) {
            return ChunkKind.EXAMPLE;
        }
        return order == 0 && section.isIntroduction() ? ChunkKind.DEFINITION : ChunkKind.PROSE;
    }

    private static boolean isTakeaway(HtmlSection section) {
        String heading = section.heading() == null ? "" : section.heading().toLowerCase(Locale.ROOT);
        return heading.contains(TAKEAWAY) || section.text().toLowerCase(Locale.ROOT).startsWith(TAKEAWAY);
    }

    private static boolean isExample(String paragraph) {
        String lower = paragraph.trim().toLowerCase(Locale.ROOT);
        for (String lead : EXAMPLE_LEADS) {
            if (lower.startsWith(lead)) {
                return true;
            }
        }
        return MusicSymbols.looksLikeSymbolRun(paragraph);
    }

    private static List<String> paragraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        for (String block : text.split("\n\\s*\n")) {
            String trimmed = block.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }

    private static void append(StringBuilder current, String paragraph) {
        if (!current.isEmpty()) {
            current.append("\n\n");
        }
        current.append(paragraph);
    }

    /** Folds a passage too short to stand alone into the next one from the same section. */
    private List<TextChunk> merge(List<TextChunk> chunks) {
        List<TextChunk> merged = new ArrayList<>();
        for (TextChunk chunk : chunks) {
            boolean tooShort = chunk.body().length() < policy.minChars();
            boolean mergeable = !merged.isEmpty()
                    && merged.get(merged.size() - 1).sectionOrder() == chunk.sectionOrder()
                    && chunk.kind() != ChunkKind.TAKEAWAY;
            if (tooShort && mergeable) {
                TextChunk previous = merged.remove(merged.size() - 1);
                merged.add(new TextChunk(previous.sectionTitle(), previous.sectionOrder(),
                        previous.chunkOrder(), previous.kind(),
                        previous.body() + "\n\n" + chunk.body()));
            } else {
                merged.add(chunk);
            }
        }
        return merged;
    }
}
