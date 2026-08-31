package fr.lapetina.music.theory;

/**
 * Turns a Roman numeral into actual notes in an actual key.
 *
 * <p>The interesting case is the applied chord. {@code V7/V} does not mean "the fifth
 * degree with a seventh"; it means "go and stand in the key of the fifth degree, and take
 * the dominant seventh of <em>that</em>". So the target is realised first, a temporary key
 * is built on it, and the numeral is read there. In C major that gives D F sharp A C, with
 * the F sharp the key does not contain, which is exactly what makes it worth teaching.
 */
public final class RomanNumeralAnalyzer {

    private RomanNumeralAnalyzer() {}

    public static RomanNumeralAnalysis analyze(String numeral, Key key) {
        return analyze(RomanNumeral.parse(numeral), key);
    }

    public static RomanNumeralAnalysis analyze(RomanNumeral numeral, Key key) {
        Chord chord = realize(numeral, key);
        Integer target = numeral.appliedToDegree().orElse(null);
        boolean diatonic = !numeral.isChromatic() && !numeral.isSecondary()
                && chord.pitchClasses().stream().allMatch(key::contains);
        return new RomanNumeralAnalysis(numeral, key, chord, numeral.function(), target, diatonic);
    }

    public static Chord realize(String numeral, Key key) {
        return realize(RomanNumeral.parse(numeral), key);
    }

    public static Chord realize(RomanNumeral numeral, Key key) {
        Key context = numeral.isSecondary() ? tonicizedKey(numeral.appliedTo(), key) : key;
        RomanNumeral local = numeral.isSecondary()
                ? new RomanNumeral(numeral.accidental(), numeral.degree(), numeral.quality(),
                        numeral.inversion(), null)
                : numeral;
        return new Chord(local.rootIn(context), numeral.quality(), numeral.inversion());
    }

    /**
     * The key a chord is applied to. A lower-case target is a minor key: {@code V/ii} in C
     * tonicizes D minor, so its dominant has a C sharp rather than a C.
     */
    private static Key tonicizedKey(RomanNumeral target, Key key) {
        PitchClass root = target.rootIn(key);
        boolean minor = switch (target.quality()) {
            case MINOR, MINOR_SEVENTH, DIMINISHED, DIMINISHED_SEVENTH, HALF_DIMINISHED_SEVENTH,
                 MINOR_MAJOR_SEVENTH, MINOR_NINTH, MINOR_SIXTH -> true;
            default -> false;
        };
        return new Key(root, minor ? Mode.MINOR : Mode.MAJOR);
    }
}
