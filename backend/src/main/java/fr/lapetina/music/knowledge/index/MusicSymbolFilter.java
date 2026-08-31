package fr.lapetina.music.knowledge.index;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * Keeps only the tokens that are harmonic symbols, and makes compound ones findable by
 * their parts.
 *
 * <p>{@code V7/V} is emitted as {@code V7/V}, then {@code V7} and {@code V} at the same
 * position. So a search for {@code V7} still reaches a passage about applied dominants,
 * while a search for the exact symbol scores it far higher.
 */
public final class MusicSymbolFilter extends TokenFilter {

    private final CharTermAttribute term = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute position = addAttribute(PositionIncrementAttribute.class);
    private final Deque<String> pending = new ArrayDeque<>();

    public MusicSymbolFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!pending.isEmpty()) {
            term.setEmpty().append(pending.removeFirst());
            position.setPositionIncrement(0);
            return true;
        }
        while (input.incrementToken()) {
            String token = MusicSymbols.normalise(term.toString());
            if (!MusicSymbols.isSymbol(token)) {
                continue;
            }
            List<String> parts = MusicSymbols.expand(token);
            pending.addAll(parts);
            term.setEmpty().append(token);
            position.setPositionIncrement(1);
            return true;
        }
        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        pending.clear();
    }
}
