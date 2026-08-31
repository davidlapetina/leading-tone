package fr.lapetina.music.theory;

import java.util.Locale;
import java.util.Optional;

/**
 * A functional label for a chord within a key.
 *
 * <p>Three things it has to be able to say, which a bare scale degree cannot:
 *
 * <ul>
 *   <li>a chromatic root — {@code bII} for the Neapolitan, {@code bVI} for a borrowed
 *       submediant, {@code #iv°} — hence the accidental;
 *   <li>what a chord is applied to, as a numeral rather than a piece of text, so
 *       {@code V7/V} can be realised rather than merely printed;
 *   <li>the augmented sixths, which are named after a nationality rather than a degree.
 * </ul>
 *
 * <p><strong>The accidental convention</strong>, which is the part that is easy to get
 * wrong: a numeral with no accidental names the key's own scale degree, so {@code III} in
 * C minor is E flat. A numeral with an accidental is measured from the <em>major</em>
 * scale of the tonic, so {@code bVI} is A flat in C major and A flat in C minor alike. The
 * other reading — lowering the key's own degree — would make flat-six in C minor an A
 * double flat, which is not what anybody writing {@code bVI} means.
 */
public record RomanNumeral(
        Accidental accidental, int degree, ChordQuality quality, Inversion inversion, RomanNumeral appliedTo) {

    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI", "VII"};

    public RomanNumeral {
        if (degree < 1 || degree > 7) {
            throw new IllegalArgumentException("Scale degree out of range: " + degree);
        }
        accidental = accidental == null ? Accidental.NATURAL : accidental;
    }

    public static RomanNumeral of(int degree, ChordQuality quality, Inversion inversion) {
        return new RomanNumeral(Accidental.NATURAL, degree, quality, inversion, null);
    }

    public static RomanNumeral of(Accidental accidental, int degree, ChordQuality quality, Inversion inversion) {
        return new RomanNumeral(accidental, degree, quality, inversion, null);
    }

    public static RomanNumeral applied(int degree, ChordQuality quality, Inversion inversion, String target) {
        return new RomanNumeral(Accidental.NATURAL, degree, quality, inversion, parse(target));
    }

    public static RomanNumeral applied(int degree, ChordQuality quality, Inversion inversion, RomanNumeral target) {
        return new RomanNumeral(Accidental.NATURAL, degree, quality, inversion, target);
    }

    public static String numeralFor(int degree, boolean upperCase) {
        String numeral = NUMERALS[Math.floorMod(degree - 1, 7)];
        return upperCase ? numeral : numeral.toLowerCase(Locale.ROOT);
    }

    public boolean isSecondary() {
        return appliedTo != null;
    }

    public boolean isChromatic() {
        return accidental != Accidental.NATURAL || isAugmentedSixth();
    }

    public boolean isAugmentedSixth() {
        return quality == ChordQuality.ITALIAN_SIXTH
                || quality == ChordQuality.FRENCH_SIXTH
                || quality == ChordQuality.GERMAN_SIXTH;
    }

    /** The degree this chord is applied to, or empty when it is not a secondary chord. */
    public Optional<Integer> appliedToDegree() {
        return Optional.ofNullable(appliedTo).map(RomanNumeral::degree);
    }

    /**
     * The root this numeral names in a key.
     *
     * <p>See the class note on the accidental convention: a plain numeral takes the key's
     * own degree, an altered one is measured from the major scale.
     */
    public PitchClass rootIn(Key key) {
        if (accidental == Accidental.NATURAL) {
            boolean raisedSeventh = key.mode() == Mode.MINOR && (degree == 5 || degree == 7);
            return key.scale(raisedSeventh).degree(degree);
        }
        return new Scale(key.tonic(), ScaleType.MAJOR).degree(degree).alter(accidental.offset());
    }

    public String symbol() {
        if (isAugmentedSixth()) {
            return quality.symbol();
        }
        // Upper case for a major third, lower case for a minor one — extensions above the
        // seventh do not change which it is.
        boolean upperCase = switch (quality) {
            case MINOR, DIMINISHED, MINOR_SEVENTH, MINOR_MAJOR_SEVENTH, HALF_DIMINISHED_SEVENTH,
                 DIMINISHED_SEVENTH, MINOR_SIXTH, MINOR_NINTH, MINOR_ADD_NINE -> false;
            default -> true;
        };
        StringBuilder builder = new StringBuilder(accidental.symbol());
        builder.append(numeralFor(degree, upperCase));
        builder.append(switch (quality) {
            case DIMINISHED, DIMINISHED_SEVENTH -> "°";
            case HALF_DIMINISHED_SEVENTH -> "ø";
            case AUGMENTED, AUGMENTED_SEVENTH -> "+";
            case MAJOR_SEVENTH -> "M";
            case SUS2 -> "sus2";
            case SUS4 -> "sus4";
            default -> "";
        });
        builder.append(inversion.figuredBass(quality.size()));
        if (appliedTo != null) {
            builder.append('/').append(appliedTo.symbol());
        }
        return builder.toString();
    }

    public HarmonicFunction function() {
        if (isSecondary()) {
            return HarmonicFunction.APPLIED_DOMINANT;
        }
        if (isChromatic()) {
            return HarmonicFunction.CHROMATIC;
        }
        return HarmonicFunction.forDegree(degree);
    }

    // ------------------------------------------------------------------ parsing

    /**
     * Reads a Roman numeral as it is actually written, in textbooks and in corpus
     * annotations alike: {@code V7/V}, {@code viiø7}, {@code I64}, {@code bII6},
     * {@code Ger+6}, {@code #iv°}, and the corpus spellings {@code viio7} and {@code Ger6}.
     */
    public static RomanNumeral parse(String text) {
        return tryParse(text).orElseThrow(
                () -> new IllegalArgumentException("Unreadable Roman numeral: " + text));
    }

    public static Optional<RomanNumeral> tryParse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String input = normalise(text);

        // An applied chord is read right to left: the target is itself a numeral.
        int slash = input.lastIndexOf('/');
        if (slash > 0 && slash < input.length() - 1) {
            Optional<RomanNumeral> target = tryParse(input.substring(slash + 1));
            Optional<RomanNumeral> chord = tryParse(input.substring(0, slash));
            if (target.isPresent() && chord.isPresent()) {
                RomanNumeral base = chord.get();
                return Optional.of(new RomanNumeral(
                        base.accidental(), base.degree(), base.quality(), base.inversion(), target.get()));
            }
            return Optional.empty();
        }

        Optional<RomanNumeral> augmentedSixth = augmentedSixth(input);
        if (augmentedSixth.isPresent()) {
            return augmentedSixth;
        }
        if (input.equals("N") || input.equals("N6")) {
            // The Neapolitan is a flat-two major triad, conventionally in first inversion.
            return Optional.of(new RomanNumeral(Accidental.FLAT, 2, ChordQuality.MAJOR,
                    input.equals("N6") ? Inversion.FIRST : Inversion.ROOT_POSITION, null));
        }

        int at = 0;
        StringBuilder accidentalText = new StringBuilder();
        while (at < input.length() && (input.charAt(at) == 'b' || input.charAt(at) == '#')) {
            accidentalText.append(input.charAt(at));
            at++;
        }
        // A lone "b" is the note B, not a flat with nothing after it.
        if (at >= input.length()) {
            return Optional.empty();
        }
        Accidental accidental = accidentalText.isEmpty()
                ? Accidental.NATURAL
                : Accidental.parse(accidentalText.toString());

        int numeralStart = at;
        while (at < input.length() && isNumeralChar(input.charAt(at))) {
            at++;
        }
        String numeral = input.substring(numeralStart, at);
        if (numeral.isEmpty() || !sameCase(numeral)) {
            return Optional.empty();
        }
        int degree = degreeOf(numeral);
        if (degree == 0) {
            return Optional.empty();
        }
        boolean minorNumeral = Character.isLowerCase(numeral.charAt(0));
        String suffix = input.substring(at);
        return quality(minorNumeral, degree, suffix).map(resolved ->
                new RomanNumeral(accidental, degree, resolved.quality(), resolved.inversion(), null));
    }

    private record Resolved(ChordQuality quality, Inversion inversion) {}

    private static Optional<Resolved> quality(boolean minorNumeral, int degree, String suffix) {
        String marks = suffix;
        ChordQuality triad = minorNumeral ? ChordQuality.MINOR : ChordQuality.MAJOR;
        boolean halfDiminished = false;
        boolean diminished = false;

        while (!marks.isEmpty()) {
            if (marks.startsWith("°") || marks.startsWith("o")) {
                diminished = true;
                marks = marks.substring(1);
            } else if (marks.startsWith("dim")) {
                diminished = true;
                marks = marks.substring(3);
            } else if (marks.startsWith("ø")) {
                halfDiminished = true;
                marks = marks.substring(1);
            } else if (marks.startsWith("+") || marks.startsWith("aug")) {
                triad = ChordQuality.AUGMENTED;
                marks = marks.startsWith("+") ? marks.substring(1) : marks.substring(3);
            } else if (marks.startsWith("sus4")) {
                triad = ChordQuality.SUS4;
                marks = marks.substring(4);
            } else if (marks.startsWith("sus2")) {
                triad = ChordQuality.SUS2;
                marks = marks.substring(4);
            } else if (marks.startsWith("maj") || marks.startsWith("M")) {
                triad = ChordQuality.MAJOR_SEVENTH;
                marks = marks.startsWith("maj") ? marks.substring(3) : marks.substring(1);
            } else {
                break;
            }
        }
        if (diminished) {
            triad = ChordQuality.DIMINISHED;
        }
        if (halfDiminished) {
            triad = ChordQuality.HALF_DIMINISHED_SEVENTH;
        }

        String figure = marks;
        return switch (figure) {
            case "" -> Optional.of(new Resolved(triad, Inversion.ROOT_POSITION));
            case "6" -> Optional.of(new Resolved(triad, Inversion.FIRST));
            case "64" -> Optional.of(new Resolved(triad, Inversion.SECOND));
            case "7" -> Optional.of(new Resolved(seventhFor(triad, minorNumeral, degree), Inversion.ROOT_POSITION));
            case "65" -> Optional.of(new Resolved(seventhFor(triad, minorNumeral, degree), Inversion.FIRST));
            case "43" -> Optional.of(new Resolved(seventhFor(triad, minorNumeral, degree), Inversion.SECOND));
            case "42", "2" -> Optional.of(new Resolved(seventhFor(triad, minorNumeral, degree), Inversion.THIRD));
            case "9" -> Optional.of(new Resolved(
                    minorNumeral ? ChordQuality.MINOR_NINTH : ChordQuality.DOMINANT_NINTH, Inversion.ROOT_POSITION));
            case "11" -> Optional.of(new Resolved(ChordQuality.DOMINANT_ELEVENTH, Inversion.ROOT_POSITION));
            case "13" -> Optional.of(new Resolved(ChordQuality.DOMINANT_THIRTEENTH, Inversion.ROOT_POSITION));
            default -> Optional.empty();
        };
    }

    /**
     * What a bare "7" means, which depends on where you are.
     *
     * <p>{@code V7} is a dominant seventh and {@code ii7} a minor seventh, because the
     * figure means "add the seventh this key gives you". On a major numeral that is a major
     * seventh, except on the fifth and seventh degrees where the diatonic seventh is minor.
     * Writing {@code IM7} or {@code Imaj7} says major seventh explicitly.
     */
    private static ChordQuality seventhFor(ChordQuality triad, boolean minorNumeral, int degree) {
        return switch (triad) {
            case MINOR -> ChordQuality.MINOR_SEVENTH;
            case DIMINISHED -> ChordQuality.DIMINISHED_SEVENTH;
            case HALF_DIMINISHED_SEVENTH -> ChordQuality.HALF_DIMINISHED_SEVENTH;
            case AUGMENTED -> ChordQuality.AUGMENTED_SEVENTH;
            case MAJOR_SEVENTH -> ChordQuality.MAJOR_SEVENTH;
            case SUS4 -> ChordQuality.DOMINANT_SEVENTH_SUS4;
            default -> minorNumeral || degree == 5 || degree == 7
                    ? ChordQuality.DOMINANT_SEVENTH
                    : ChordQuality.MAJOR_SEVENTH;
        };
    }

    private static Optional<RomanNumeral> augmentedSixth(String input) {
        ChordQuality quality = switch (input) {
            case "It+6", "It6", "It" -> ChordQuality.ITALIAN_SIXTH;
            case "Fr+6", "Fr6", "Fr" -> ChordQuality.FRENCH_SIXTH;
            case "Ger+6", "Ger6", "Ger" -> ChordQuality.GERMAN_SIXTH;
            default -> null;
        };
        // An augmented sixth is built on the lowered sixth degree, whatever it is called.
        return Optional.ofNullable(quality).map(
                found -> new RomanNumeral(Accidental.FLAT, 6, found, Inversion.ROOT_POSITION, null));
    }

    /** Folds the ways the same numeral gets written, without touching case, which is meaning. */
    private static String normalise(String text) {
        return text.trim()
                .replace('♭', 'b')
                .replace('♯', '#')
                .replace('°', '°')
                .replace('∅', 'ø')
                .replace("(", "")
                .replace(")", "");
    }

    private static boolean isNumeralChar(char c) {
        char upper = Character.toUpperCase(c);
        return upper == 'I' || upper == 'V';
    }

    /** "iV" is a typo, not a numeral: a numeral is written in one case throughout. */
    private static boolean sameCase(String numeral) {
        boolean lower = Character.isLowerCase(numeral.charAt(0));
        for (int i = 1; i < numeral.length(); i++) {
            if (Character.isLowerCase(numeral.charAt(i)) != lower) {
                return false;
            }
        }
        return true;
    }

    private static int degreeOf(String numeral) {
        String upper = numeral.toUpperCase(Locale.ROOT);
        for (int i = 0; i < NUMERALS.length; i++) {
            if (NUMERALS[i].equals(upper)) {
                return i + 1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return symbol();
    }
}
