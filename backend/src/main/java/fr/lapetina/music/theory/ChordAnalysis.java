package fr.lapetina.music.theory;

/** One chord seen through the lens of a key. {@code romanNumeral} is null when nothing fits. */
public record ChordAnalysis(Chord chord, RomanNumeral romanNumeral, HarmonicFunction function, boolean diatonic) {

    public String romanNumeralSymbol() {
        return romanNumeral == null ? "?" : romanNumeral.symbol();
    }
}
