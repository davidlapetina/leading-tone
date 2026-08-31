package fr.lapetina.music.knowledge.ingestion.jht;

/**
 * Translates the Jazz Harmony Treebank's chord spelling into the one this engine reads.
 *
 * <p>The treebank uses the shorthand of a lead sheet: {@code ^} for a major seventh,
 * {@code %} for half-diminished, {@code o} for diminished, {@code -} for minor. Those are
 * not decoration — {@code C^7} and {@code C7} are different chords, and reading one as the
 * other would silently corrupt every harmonic analysis derived from this corpus.
 */
public final class JazzChordSymbols {

    private JazzChordSymbols() {}

    /** {@code G^7} becomes {@code Gmaj7}, {@code A%7} becomes {@code Am7b5}, and so on. */
    public static String toLeadSheet(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        String text = symbol.trim();
        int rootEnd = 1;
        while (rootEnd < text.length() && (text.charAt(rootEnd) == '#' || text.charAt(rootEnd) == 'b')) {
            rootEnd++;
        }
        String root = text.substring(0, rootEnd);
        String suffix = text.substring(rootEnd);

        String quality = switch (suffix) {
            case "" -> "";
            case "^" -> "";                 // a bare caret is a major triad
            case "^7" -> "maj7";
            case "^9" -> "maj9";
            case "6" -> "6";
            case "69", "6/9" -> "6/9";
            case "-" -> "m";
            case "-7" -> "m7";
            case "-6" -> "m6";
            case "-^7" , "m^7", "-maj7" -> "mMaj7";
            case "%7", "%" -> "m7b5";
            case "o7", "o" -> "dim7";
            case "+" -> "aug";
            case "+7" -> "7#5";
            case "sus" , "sus4" -> "sus4";
            case "7sus", "7sus4" -> "7sus4";
            case "9" -> "9";
            case "11" -> "11";
            case "13" -> "13";
            case "7b9" -> "7b9";
            case "7#9" -> "7#9";
            case "7#11" -> "7#11";
            case "7b13" -> "7b13";
            case "7" -> "7";
            default -> suffix
                    .replace("^", "maj")
                    .replace("%", "m7b5")
                    .replace("-", "m");
        };
        return root + quality;
    }

    /**
     * The treebank writes a minor key in lower case, so {@code c} is C minor and {@code C}
     * is C major. Losing that distinction would put every Roman numeral in the wrong mode.
     */
    public static String toKeyName(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        // The treebank writes a flat as a hyphen in the key field, so "E-" is E flat and
        // "b-" is B flat minor. Chord symbols in the same file use "b", which is why this
        // translation belongs here and not in the chord parser.
        String text = key.trim().replace("-", "b");
        boolean minor = Character.isLowerCase(text.charAt(0));
        String tonic = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        return tonic + (minor ? " minor" : " major");
    }
}
