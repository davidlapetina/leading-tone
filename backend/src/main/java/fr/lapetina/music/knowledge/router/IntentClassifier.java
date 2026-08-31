package fr.lapetina.music.knowledge.router;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Works out what kind of answer a question wants.
 *
 * <p>Deliberately deterministic, for the same reason {@code FocusDetector} is: this decides
 * where a turn goes, and a routing decision made by a language model cannot be tested,
 * cannot be reproduced from a bug report, and stops working when the model is switched off.
 * The whole application is built to keep teaching without one.
 */
@ApplicationScoped
public class IntentClassifier {

    private static final Pattern CALCULATION = Pattern.compile(
            "\\b(what|which)\\s+(notes?|pitches|chord|scale|key|interval)\\b"
                    + "|\\bspell\\b|\\bbuild\\b|\\btranspose\\b"
                    + "|\\bwhat\\s+is\\s+(the\\s+)?[b#]?(i{1,3}|iv|vi{0,3})[^\\s]*\\s+in\\b"
                    + "|\\b(above|below)\\s+[A-G][b#]?\\b"
                    + "|\\bnotes?\\s+(in|of)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXAMPLE = Pattern.compile(
            "\\b(example|excerpt|passage|show me|find me|real music|in a real|actual)\\b"
                    + "|\\b(beethoven|mozart|chopin|corelli|debussy|dvo|grieg|liszt|medtner|schumann|tchaikovsky)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXPLANATION = Pattern.compile(
            "\\b(why|explain|how does|how do|what is|what are|what does|meaning of|difference between|purpose of)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXERCISE = Pattern.compile(
            "\\b(exercise|quiz|test me|practise|practice|drill|give me a question|ask me)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DIAGNOSTIC = Pattern.compile(
            "\\b(what do i know|how am i doing|my progress|what should i (learn|study)|am i ready|weak)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * The intents a message carries, in no particular order and possibly several.
     *
     * <p>An empty question, or one that matches nothing, is treated as asking for an
     * explanation: that is the least presumptuous default and the one that degrades best.
     */
    public Set<RetrievalIntent> classify(String message) {
        Set<RetrievalIntent> intents = EnumSet.noneOf(RetrievalIntent.class);
        if (message == null || message.isBlank()) {
            return intents;
        }
        String text = message.toLowerCase(Locale.ROOT);
        if (DIAGNOSTIC.matcher(text).find()) {
            intents.add(RetrievalIntent.STUDENT_DIAGNOSTIC);
        }
        if (EXERCISE.matcher(text).find()) {
            intents.add(RetrievalIntent.EXERCISE_REQUEST);
        }
        if (EXAMPLE.matcher(text).find()) {
            intents.add(RetrievalIntent.HARMONIC_EXAMPLE);
        }
        if (CALCULATION.matcher(text).find() || TheoryQuestion.looksCalculable(message)) {
            intents.add(RetrievalIntent.DETERMINISTIC_CALCULATION);
        }
        if (EXPLANATION.matcher(text).find()) {
            intents.add(RetrievalIntent.CONCEPT_EXPLANATION);
        }
        if (intents.isEmpty()) {
            intents.add(RetrievalIntent.CONCEPT_EXPLANATION);
        }
        return intents;
    }

}
