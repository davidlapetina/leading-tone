package fr.lapetina.music.knowledge.index;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Recognises the symbols of harmonic analysis in ordinary text.
 *
 * <p>Kept free of Lucene so it can be tested on its own: the hard part of symbol-aware
 * search is deciding what counts as a symbol, and that deserves a test that does not need
 * an index to run.
 */
public final class MusicSymbols {

    /** Roman numerals with optional accidental, quality, figure and applied target. */
    private static final Pattern ROMAN = Pattern.compile(
            "^(?:[b#]{0,2})(?:i{1,3}|iv|vi{0,3}|I{1,3}|IV|VI{0,3})"
                    + "(?:°|o|ø|\\+|dim|aug|M|maj|m)?"
                    + "(?:7|6|64|65|43|42|9|11|13)?"
                    + "(?:/(?:[b#]{0,2})(?:i{1,3}|iv|vi{0,3}|I{1,3}|IV|VI{0,3}))?$");

    /** Chains such as ii-V-I, written with hyphens or dashes. */
    private static final Pattern CHAIN = Pattern.compile(
            "^(?:[b#]{0,2}(?:i{1,3}|iv|vi{0,3}|I{1,3}|IV|VI{0,3})[^\\s-]{0,4})"
                    + "(?:-(?:[b#]{0,2}(?:i{1,3}|iv|vi{0,3}|I{1,3}|IV|VI{0,3})[^\\s-]{0,4})){1,4}$");

    /** The augmented sixths and the Neapolitan, which do not follow the roman pattern. */
    private static final Set<String> NAMED = Set.of(
            "It+6", "Fr+6", "Ger+6", "N6", "N", "It6", "Fr6", "Ger6");

    private MusicSymbols() {}

    /** Normalises the many ways publishers write the same sign. */
    public static String normalise(String token) {
        return token.replace('♭', 'b')   // ♭
                .replace('♯', '#')       // ♯
                .replace('♮', 'n')       // ♮
                .replace('–', '-')       // en dash
                .replace('—', '-')       // em dash
                .replace('−', '-');      // minus sign
    }

    public static boolean isSymbol(String token) {
        if (token == null || token.isBlank() || token.length() > 20) {
            return false;
        }
        String t = normalise(token);
        return NAMED.contains(t) || ROMAN.matcher(t).matches() || CHAIN.matcher(t).matches();
    }

    /**
     * The symbols in a piece of text, in order and without duplicates. Used to build the
     * exact-match half of a query from whatever the learner actually typed.
     */
    public static List<String> extract(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null) {
            return List.of();
        }
        for (String raw : text.split("[\\s,.;:()\\[\\]\"']+")) {
            String token = normalise(raw.trim());
            if (isSymbol(token)) {
                found.add(token);
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * The extra tokens a compound symbol should also be findable by, so that a search for
     * {@code V7} still reaches a passage about {@code V7/V} while an exact search for
     * {@code V7/V} scores far higher.
     */
    public static List<String> expand(String symbol) {
        List<String> parts = new ArrayList<>();
        String t = normalise(symbol);
        if (t.contains("-")) {
            for (String piece : t.split("-")) {
                if (!piece.isBlank()) {
                    parts.add(piece);
                }
            }
        } else if (t.contains("/")) {
            int slash = t.indexOf('/');
            parts.add(t.substring(0, slash));
            parts.add(t.substring(slash + 1));
        }
        parts.remove(t);
        return parts;
    }

    /** True when a line is mostly notation rather than prose, so it stays with its explanation. */
    public static boolean looksLikeSymbolRun(String line) {
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length == 0 || tokens.length > 24) {
            return false;
        }
        long symbols = 0;
        for (String token : tokens) {
            if (isSymbol(token.replaceAll("[,.;:]$", ""))) {
                symbols++;
            }
        }
        return symbols * 10 >= tokens.length * 6;
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
