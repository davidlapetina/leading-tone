package fr.lapetina.music.theory;

import java.util.List;

/**
 * A Roman numeral worked out in a key: what it is, what notes it is, and what it is doing.
 *
 * <p>This is the structured form the tutor reasons about. The language model turns it into
 * a sentence; it does not compute it, because the answer to "what is V7/V in C major" is
 * arithmetic and arithmetic should not be guessed.
 */
public record RomanNumeralAnalysis(
        RomanNumeral numeral,
        Key key,
        Chord chord,
        HarmonicFunction function,
        Integer targetDegree,
        boolean diatonic) {

    public PitchClass root() {
        return chord.root();
    }

    public ChordQuality quality() {
        return chord.quality();
    }

    public List<PitchClass> pitchClasses() {
        return chord.pitchClasses();
    }

    public String input() {
        return numeral.symbol();
    }

    /** The spelled notes, e.g. {@code "D F# A C"}. Spelling, not pitch class. */
    public String spelling() {
        return String.join(" ", pitchClasses().stream().map(PitchClass::name).toList());
    }

    public String describe() {
        StringBuilder text = new StringBuilder(numeral.symbol())
                .append(" in ").append(key.name())
                .append(" is ").append(chord.symbol())
                .append(" (").append(spelling()).append(')');
        if (targetDegree != null) {
            text.append(", the dominant of ").append(RomanNumeral.numeralFor(targetDegree, true));
        }
        return text.toString();
    }
}
