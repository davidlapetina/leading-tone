package fr.lapetina.music.exercise;

import fr.lapetina.music.theory.PitchClass;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns what a person typed into something comparable.
 *
 * <p>People write "E flat", "Eb", "E-flat" and "e♭" for the same note, and answer
 * questions with whole sentences. None of that should read as a mistake.
 */
public final class AnswerNormalizer {

    private static final Pattern NOTE_TOKEN =
            Pattern.compile("(?<![A-Za-z])([A-Ga-g])(##|bb|#|b|x)?(?![A-Za-z])");

    // "a" and "i" are deliberately absent: A is a note and I is a Roman numeral.
    private static final List<String> FILLER = List.of(
            "the", "an", "is", "it", "its", "chord", "triad", "note", "notes", "answer",
            "would", "be", "think", "that", "and");

    /** Ways of saying "tell me more" rather than answering. */
    private static final List<String> HELP_PHRASES = List.of(
            "explain", "explain it", "explain that", "explain more", "tell me more", "more detail",
            "what do you mean", "what does that mean", "i don t understand", "i dont understand",
            "i m confused", "im confused", "confused", "help", "hint", "give me a hint", "why", "why is that",
            "how so", "huh", "what");

    /** Ways of saying "I have nothing", which is worth recording but is not a wrong answer. */
    private static final List<String> DONT_KNOW_PHRASES = List.of(
            "i don t know", "i dont know", "dont know", "don t know", "no idea", "not sure", "dunno",
            "no clue", "skip", "pass", "next", "i give up", "give up");

    /** Words that start a question rather than an answer. */
    private static final List<String> INTERROGATIVES = List.of(
            "what", "why", "how", "which", "who", "where", "when", "can", "could", "would", "should",
            "does", "do", "is", "are", "tell", "explain", "show", "whats", "hows");

    private AnswerNormalizer() {
    }

    /**
     * True when the text reads as a question being asked rather than an answer being
     * given.
     *
     * <p>Deliberately strict: it requires an opening interrogative, so "predominant?" is
     * still treated as an answer while "what is a C major add 7 chord" is not. Getting
     * this wrong in the lenient direction would let real answers go ungraded; getting it
     * wrong in the strict direction marks a learner's question as a mistake.
     */
    public static boolean isQuestion(String raw) {
        String normalized = stripPunctuation(raw);
        if (normalized.isEmpty()) {
            return false;
        }
        String first = normalized.split(" ")[0];
        return INTERROGATIVES.contains(first);
    }

    /**
     * True when the learner asked for help rather than answering.
     *
     * <p>This matters more than it looks: without it, typing "explain" is marked wrong and
     * recorded as evidence that the learner does not know the concept, which is the exact
     * opposite of what asking for help means.
     */
    public static boolean isRequestForHelp(String raw) {
        String normalized = stripPunctuation(raw);
        return !normalized.isEmpty() && HELP_PHRASES.contains(normalized);
    }

    /** True when the learner said they do not know, which is a skip rather than a mistake. */
    public static boolean isDontKnow(String raw) {
        String normalized = stripPunctuation(raw);
        return !normalized.isEmpty() && DONT_KNOW_PHRASES.contains(normalized);
    }

    private static String stripPunctuation(String raw) {
        return raw == null ? "" : raw.toLowerCase()
                .replaceAll("[^a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Lower-cases, expands "flat"/"sharp", and drops filler words and punctuation. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.toLowerCase()
                .replace('♯', '#')
                .replace('♭', 'b')
                .replace('°', 'o')
                .replace("-flat", "b")
                .replace(" flat", "b")
                .replace("-sharp", "#")
                .replace(" sharp", "#")
                .replace("-", " ")
                .replaceAll("[^a-z0-9#/ ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        List<String> kept = new ArrayList<>();
        for (String word : text.split(" ")) {
            if (!word.isEmpty() && !FILLER.contains(word)) {
                kept.add(word);
            }
        }
        return String.join(" ", kept);
    }

    /**
     * True when the expected phrase appears in the answer as whole words.
     *
     * <p>Whole words, not a substring: otherwise "Eb" would count as an answer of "E",
     * and a near miss on spelling is exactly what this application must not wave through.
     */
    public static boolean matches(String rawAnswer, String expected) {
        List<String> answer = tokens(rawAnswer);
        List<String> target = tokens(expected);
        if (target.isEmpty() || answer.size() < target.size()) {
            return false;
        }
        for (int start = 0; start + target.size() <= answer.size(); start++) {
            if (answer.subList(start, start + target.size()).equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> tokens(String raw) {
        String normalized = normalize(raw);
        return normalized.isEmpty() ? List.of() : List.of(normalized.split(" "));
    }

    /** Pulls spelled note names out of free text: "E flat, G and B flat" gives Eb G Bb. */
    public static List<PitchClass> notesIn(String raw) {
        String text = (raw == null ? "" : raw)
                .replace('♯', '#')
                .replace('♭', 'b')
                .replaceAll("(?i)-?\\s*flat", "b")
                .replaceAll("(?i)-?\\s*sharp", "#")
                .replaceAll("(?i)\\bdouble\\s*b", "bb")
                .replaceAll("[,;]", " ");
        List<PitchClass> notes = new ArrayList<>();
        Matcher matcher = NOTE_TOKEN.matcher(text);
        while (matcher.find()) {
            String letter = matcher.group(1).toUpperCase();
            String accidental = matcher.group(2) == null ? "" : matcher.group(2);
            try {
                notes.add(PitchClass.parse(letter + accidental));
            } catch (IllegalArgumentException notANote) {
                // Skip anything that looked like a note but is not one.
            }
        }
        return notes;
    }
}
