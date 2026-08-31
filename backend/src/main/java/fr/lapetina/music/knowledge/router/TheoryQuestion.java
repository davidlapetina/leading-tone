package fr.lapetina.music.knowledge.router;

import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Interval;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.Progression;
import fr.lapetina.music.theory.RomanNumeral;
import fr.lapetina.music.theory.RomanNumeralAnalysis;
import fr.lapetina.music.theory.RomanNumeralAnalyzer;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises questions with an arithmetic answer, and answers them.
 *
 * <p>This is the deterministic-first rule made concrete. "What is V7/V in C major" has one
 * correct answer, D F♯ A C, and the way to get it right every time is to compute it. A
 * language model asked the same question will usually be right and occasionally be
 * confidently wrong, and a learner has no way to tell the two apart.
 *
 * <p>Nothing here is a fallback for the model. If the question is recognised, the answer is
 * computed and the model's job is to explain it, not to check it.
 */
public final class TheoryQuestion {

    /** A key named anywhere in the sentence: "in C major", "in f# minor", "in Bb". */
    private static final Pattern IN_KEY = Pattern.compile(
            "\\bin\\s+([A-Ga-g][b#\\u266d\\u266f]?)\\s*(major|minor|maj|min)?\\b");

    private static final Pattern ROMAN = Pattern.compile(
            "\\b((?:[b#]{0,2})(?:VII|VI|IV|V|III|II|I|vii|vi|iv|v|iii|ii|i)"
                    + "(?:\\u00b0|o|\\u00f8|\\+|dim|aug|M|maj)?(?:7|6|64|65|43|42|9|11|13)?"
                    + "(?:/(?:[b#]{0,2})(?:VII|VI|IV|V|III|II|I|vii|vi|iv|v|iii|ii|i))?)\\b");

    private static final Pattern AUGMENTED_SIXTH = Pattern.compile("\\b(It|Fr|Ger)\\+?6\\b");

    private static final Pattern CHORD_SYMBOL = Pattern.compile(
            "\\b([A-G][b#]{0,2}(?:maj|m|min|dim|aug|sus|add|\\u00b0|\\u00f8|\\+|-)?[0-9()#b/]{0,8})\\b");

    private static final Pattern SCALE = Pattern.compile(
            "\\b([A-G][b#]?)\\s+(major|natural minor|harmonic minor|melodic minor|dorian|phrygian|"
                    + "lydian|mixolydian|aeolian|locrian|blues|whole tone|altered|"
                    + "major pentatonic|minor pentatonic)\\s*(scale)?\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern INTERVAL_ABOVE = Pattern.compile(
            "\\b(major|minor|perfect|augmented|diminished)\\s+"
                    + "(unison|second|third|fourth|fifth|sixth|seventh|octave|ninth)\\s+"
                    + "(above|below)\\s+([A-G][b#]{0,2})\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TRANSPOSE = Pattern.compile(
            "\\btranspose\\s+(.+?)\\s+from\\s+(.+?)\\s+to\\s+(.+?)\\s*[.?]?$",
            Pattern.CASE_INSENSITIVE);

    private TheoryQuestion() {}

    /** Cheap check used by the classifier: does this look like something computable? */
    public static boolean looksCalculable(String message) {
        return message != null
                && (AUGMENTED_SIXTH.matcher(message).find()
                        || TRANSPOSE.matcher(message).find()
                        || INTERVAL_ABOVE.matcher(message).find()
                        || SCALE.matcher(message).find()
                        || (ROMAN.matcher(message).find() && IN_KEY.matcher(message).find()));
    }

    /**
     * The computed answer, or empty when nothing here is recognised.
     *
     * <p>Empty is a real outcome, not a failure: the tutor then explains rather than
     * calculating, which is right for a question that was not arithmetic.
     */
    public static Optional<TheoryAnswer> answer(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        return transposition(message)
                .or(() -> romanNumeral(message))
                .or(() -> intervalAbove(message))
                .or(() -> scale(message))
                .or(() -> chordSpelling(message));
    }

    private static Optional<TheoryAnswer> romanNumeral(String message) {
        Optional<Key> key = keyIn(message);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        Matcher augmented = AUGMENTED_SIXTH.matcher(message);
        String numeral = null;
        if (augmented.find()) {
            numeral = augmented.group(1) + "+6";
        } else {
            Matcher roman = ROMAN.matcher(message);
            while (roman.find()) {
                String candidate = roman.group(1);
                // "I" and "in" appear in ordinary English; require something chord-shaped.
                if (candidate.length() > 1 || Character.isUpperCase(candidate.charAt(0))) {
                    if (RomanNumeral.tryParse(candidate).isPresent()) {
                        numeral = candidate;
                        break;
                    }
                }
            }
        }
        if (numeral == null) {
            return Optional.empty();
        }
        try {
            RomanNumeralAnalysis analysis = RomanNumeralAnalyzer.analyze(numeral, key.get());
            return Optional.of(new TheoryAnswer(
                    TheoryAnswer.Kind.ROMAN_NUMERAL,
                    "realizeRomanNumeral(%s, %s)".formatted(numeral, key.get().name()),
                    analysis.describe(),
                    analysis.spelling(),
                    analysis));
        } catch (RuntimeException notReadable) {
            return Optional.empty();
        }
    }

    private static Optional<TheoryAnswer> chordSpelling(String message) {
        Matcher matcher = CHORD_SYMBOL.matcher(message);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate.length() < 2) {
                continue;
            }
            try {
                Chord chord = ChordAnalyzer.parse(candidate);
                String spelling = String.join(" ",
                        chord.pitchClasses().stream().map(PitchClass::name).toList());
                return Optional.of(new TheoryAnswer(
                        TheoryAnswer.Kind.CHORD,
                        "buildChord(%s)".formatted(candidate),
                        "%s is %s: %s".formatted(chord.symbol(), chord.describe(), spelling),
                        spelling,
                        chord));
            } catch (RuntimeException notAChord) {
                // Ordinary words look like chord symbols; keep looking rather than guessing.
            }
        }
        return Optional.empty();
    }

    private static Optional<TheoryAnswer> scale(String message) {
        Matcher matcher = SCALE.matcher(message);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            ScaleType type = ScaleType.valueOf(
                    matcher.group(2).trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
            Scale built = Scale.of(matcher.group(1), type);
            String spelling = String.join(" ",
                    built.pitchClasses().stream().map(PitchClass::name).toList());
            return Optional.of(new TheoryAnswer(
                    TheoryAnswer.Kind.SCALE,
                    "buildScale(%s, %s)".formatted(matcher.group(1), type),
                    "%s is %s".formatted(built.name(), spelling),
                    spelling,
                    built));
        } catch (RuntimeException notAScale) {
            return Optional.empty();
        }
    }

    private static Optional<TheoryAnswer> intervalAbove(String message) {
        Matcher matcher = INTERVAL_ABOVE.matcher(message);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            Interval interval = Interval.parse(
                    qualityLetter(matcher.group(1)) + numberOf(matcher.group(2)));
            PitchClass from = PitchClass.parse(matcher.group(4));
            boolean above = matcher.group(3).equalsIgnoreCase("above");
            PitchClass result = above
                    ? from.transpose(interval)
                    : from.transpose(inversionOf(interval));
            return Optional.of(new TheoryAnswer(
                    TheoryAnswer.Kind.INTERVAL,
                    "buildInterval(%s, %s)".formatted(from.name(), interval.symbol()),
                    "%s %s %s is %s".formatted(interval.symbol(), matcher.group(3), from.name(), result.name()),
                    result.name(),
                    result));
        } catch (RuntimeException notAnInterval) {
            return Optional.empty();
        }
    }

    private static Optional<TheoryAnswer> transposition(String message) {
        Matcher matcher = TRANSPOSE.matcher(message);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            Key from = Key.parse(matcher.group(2));
            Key to = Key.parse(matcher.group(3));
            Progression progression = Progression.parse(matcher.group(1).replace("-", " "));
            List<Chord> chords = progression.realize(to);
            String spelling = String.join(" ", chords.stream().map(Chord::symbol).toList());
            return Optional.of(new TheoryAnswer(
                    TheoryAnswer.Kind.PROGRESSION,
                    "transposeProgression(%s, %s, %s)".formatted(progression.symbol(), from.name(), to.name()),
                    "%s in %s is %s".formatted(progression.symbol(), to.name(), spelling),
                    spelling,
                    chords));
        } catch (RuntimeException notTransposable) {
            return Optional.empty();
        }
    }

    static Optional<Key> keyIn(String message) {
        Matcher matcher = IN_KEY.matcher(message);
        while (matcher.find()) {
            String mode = matcher.group(2) == null ? "major" : matcher.group(2);
            try {
                return Optional.of(Key.parse(matcher.group(1) + " " + mode));
            } catch (RuntimeException notAKey) {
                // "in the" and similar; keep looking.
            }
        }
        return Optional.empty();
    }

    private static String qualityLetter(String word) {
        return switch (word.toLowerCase(Locale.ROOT)) {
            case "major" -> "M";
            case "minor" -> "m";
            case "perfect" -> "P";
            case "augmented" -> "A";
            default -> "d";
        };
    }

    private static String numberOf(String word) {
        return switch (word.toLowerCase(Locale.ROOT)) {
            case "unison" -> "1";
            case "second" -> "2";
            case "third" -> "3";
            case "fourth" -> "4";
            case "fifth" -> "5";
            case "sixth" -> "6";
            case "seventh" -> "7";
            case "ninth" -> "9";
            default -> "8";
        };
    }

    /** Downward by an interval is upward by its inversion, within the octave. */
    private static Interval inversionOf(Interval interval) {
        return Interval.parse(switch (interval.quality()) {
            case MAJOR -> "m";
            case MINOR -> "M";
            case AUGMENTED -> "d";
            case DIMINISHED -> "A";
            case PERFECT -> "P";
        } + (9 - interval.number()));
    }
}
