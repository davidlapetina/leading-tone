package fr.lapetina.music.knowledge.router;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a request for real music actually named.
 *
 * <p>"Give me a Beethoven example of V/V" names both a composer and a harmony, and the
 * corpus can be asked for exactly that. Falling back to whatever illustrates the concept
 * under discussion would return something true and irrelevant, which reads as an answer and
 * is not one.
 */
public record ExampleRequest(String romanNumeral, String cadence, String composer) {

    /** Surnames as the corpora record them, matched loosely against the composer column. */
    private static final List<String> COMPOSERS = List.of(
            "beethoven", "mozart", "chopin", "corelli", "debussy", "dvorak", "dvořák",
            "grieg", "liszt", "medtner", "schumann", "tchaikovsky");

    private static final Pattern ROMAN = Pattern.compile(
            "\\b((?:[b#]{0,2})(?:VII|VI|IV|V|III|II|I|vii|vi|iv|v|iii|ii|i)"
                    + "(?:\\u00b0|o|\\u00f8|\\+|dim|aug)?(?:7|6|64|65|43|42)?"
                    + "(?:/(?:[b#]{0,2})(?:VII|VI|IV|V|III|II|I|vii|vi|iv|v|iii|ii|i))?)\\b");

    private static final Pattern NAMED_CHORD = Pattern.compile(
            "\\b(neapolitan|german sixth|french sixth|italian sixth|augmented sixth)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final java.util.Map<String, String> CADENCES = java.util.Map.of(
            "deceptive cadence", "DC",
            "perfect authentic cadence", "PAC",
            "imperfect authentic cadence", "IAC",
            "authentic cadence", "PAC",
            "half cadence", "HC",
            "plagal cadence", "PC",
            "evaded cadence", "EC");

    public static ExampleRequest from(String message) {
        if (message == null || message.isBlank()) {
            return new ExampleRequest(null, null, null);
        }
        return new ExampleRequest(numeralIn(message), cadenceIn(message), composerIn(message));
    }

    /**
     * Whether the learner named a harmony, as opposed to only a composer.
     *
     * <p>This is the distinction that stops "a Mozart deceptive cadence" being answered with
     * the first chord of a Mozart sonata. A composer alone is not a question the corpus can
     * answer usefully; a harmony is.
     */
    public boolean namesAHarmony() {
        return romanNumeral != null || cadence != null;
    }

    public boolean isSpecific() {
        return namesAHarmony() || composer != null;
    }

    private static String cadenceIn(String message) {
        String text = message.toLowerCase(java.util.Locale.ROOT);
        return CADENCES.entrySet().stream()
                .filter(entry -> text.contains(entry.getKey()))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String composerIn(String message) {
        String text = message.toLowerCase(Locale.ROOT);
        return COMPOSERS.stream().filter(text::contains).findFirst().orElse(null);
    }

    private static String numeralIn(String message) {
        Matcher named = NAMED_CHORD.matcher(message);
        if (named.find()) {
            return switch (named.group(1).toLowerCase(Locale.ROOT)) {
                case "neapolitan" -> "bII6";
                case "german sixth" -> "Ger6";
                case "french sixth" -> "Fr6";
                case "italian sixth" -> "It6";
                default -> "Ger6";
            };
        }
        Matcher roman = ROMAN.matcher(message);
        while (roman.find()) {
            String candidate = roman.group(1);
            // A single letter is not a request: "I" is a pronoun and "V" on its own is more
            // often a stray capital than a chord. Two characters is the shortest real ask.
            if (candidate.length() > 1) {
                return candidate;
            }
        }
        return null;
    }
}
