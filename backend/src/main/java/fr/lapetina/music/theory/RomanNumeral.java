package fr.lapetina.music.theory;

/**
 * A functional label for a chord within a key. {@code appliedTo} is non-null for applied
 * (secondary) chords, so V7/V renders as {@code V7/V}.
 */
public record RomanNumeral(int degree, ChordQuality quality, Inversion inversion, String appliedTo) {

    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI", "VII"};

    public static RomanNumeral of(int degree, ChordQuality quality, Inversion inversion) {
        return new RomanNumeral(degree, quality, inversion, null);
    }

    public static RomanNumeral applied(int degree, ChordQuality quality, Inversion inversion, String target) {
        return new RomanNumeral(degree, quality, inversion, target);
    }

    public static String numeralFor(int degree, boolean upperCase) {
        String numeral = NUMERALS[Math.floorMod(degree - 1, 7)];
        return upperCase ? numeral : numeral.toLowerCase();
    }

    public boolean isSecondary() {
        return appliedTo != null;
    }

    public String symbol() {
        // Upper case for a major third, lower case for a minor one — extensions above the
        // seventh do not change which it is.
        boolean upperCase = switch (quality) {
            case MINOR, DIMINISHED, MINOR_SEVENTH, MINOR_MAJOR_SEVENTH, HALF_DIMINISHED_SEVENTH,
                 DIMINISHED_SEVENTH, MINOR_SIXTH, MINOR_NINTH -> false;
            default -> true;
        };
        StringBuilder builder = new StringBuilder(numeralFor(degree, upperCase));
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
            builder.append('/').append(appliedTo);
        }
        return builder.toString();
    }

    public HarmonicFunction function() {
        if (isSecondary()) {
            return HarmonicFunction.APPLIED_DOMINANT;
        }
        return HarmonicFunction.forDegree(degree);
    }

    @Override
    public String toString() {
        return symbol();
    }
}
