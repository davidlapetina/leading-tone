package fr.lapetina.music.theory;

import java.util.List;

/**
 * A sequence of Roman numerals, which can be realised in any key.
 *
 * <p>Kept as numerals rather than as chords because that is what a progression <em>is</em>:
 * ii-V-I is the same progression in every key, and saying so is a teaching point rather
 * than an implementation convenience. Transposing it changes the key it is realised in and
 * leaves the numerals alone.
 *
 * <p>Note that {@code ExerciseGenerator} has a private nested type of the same name. This
 * is the public one; that one is a local helper and is not related.
 */
public record Progression(List<RomanNumeral> numerals) {

    public Progression {
        numerals = List.copyOf(numerals);
    }

    /** Reads {@code "ii7 V7 Imaj7"}, or the same written with dashes. */
    public static Progression parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Empty progression");
        }
        List<RomanNumeral> parsed = new java.util.ArrayList<>();
        for (String token : text.trim().split("[\\s,|]+|\\s*[-–—]\\s*")) {
            if (!token.isBlank()) {
                parsed.add(RomanNumeral.parse(token));
            }
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Empty progression: " + text);
        }
        return new Progression(parsed);
    }

    public List<Chord> realize(Key key) {
        return numerals.stream().map(numeral -> RomanNumeralAnalyzer.realize(numeral, key)).toList();
    }

    public List<RomanNumeralAnalysis> analyze(Key key) {
        return numerals.stream().map(numeral -> RomanNumeralAnalyzer.analyze(numeral, key)).toList();
    }

    /** The chords, in a new key. The numerals do not move, because they never do. */
    public List<Chord> transpose(Key from, Key to) {
        Interval step = Interval.between(from.tonic(), to.tonic());
        return realize(from).stream().map(chord -> chord.transpose(step)).toList();
    }

    public int size() {
        return numerals.size();
    }

    public String symbol() {
        return String.join(" - ", numerals.stream().map(RomanNumeral::symbol).toList());
    }

    @Override
    public String toString() {
        return symbol();
    }
}
