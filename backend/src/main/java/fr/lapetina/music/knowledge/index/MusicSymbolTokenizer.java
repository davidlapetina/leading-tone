package fr.lapetina.music.knowledge.index;

import org.apache.lucene.analysis.util.CharTokenizer;

/**
 * A tokenizer that does not tear music notation apart.
 *
 * <p>A standard tokenizer treats {@code /}, {@code #}, {@code +}, {@code °} and {@code ø}
 * as punctuation, so {@code V7/V} becomes two tokens, {@code vii°7} loses its diminished
 * sign and collides with {@code viiø7}, and {@code ii-V-I} becomes three roman numerals
 * that match any page containing all three anywhere. Every one of those is a distinct
 * chord to a musician, and searching for them exactly is the main reason this index is
 * lexical as well as semantic.
 *
 * <p>So a token here is a maximal run of letters, digits, and the symbols that carry
 * meaning in harmonic notation.
 */
public final class MusicSymbolTokenizer extends CharTokenizer {

    @Override
    protected boolean isTokenChar(int c) {
        return Character.isLetterOrDigit(c)
                || c == '#' || c == '/' || c == '+' || c == '-'
                // The dash forms publishers actually use. These must be token characters
                // here, because normalising them later cannot undo a token already split.
                || c == '\u2013' || c == '\u2014' || c == '\u2212'
                || c == '°' || c == 'ø' || c == '♭' || c == '♯' || c == '♮' || c == 'Δ';
    }
}
