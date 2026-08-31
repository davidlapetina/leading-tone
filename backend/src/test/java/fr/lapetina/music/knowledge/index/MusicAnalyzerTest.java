package fr.lapetina.music.knowledge.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The analyzer is where symbol-aware search is won or lost, so it is tested on its own,
 * without an index.
 */
class MusicAnalyzerTest {

    private static List<String> tokens(Analyzer analyzer, String field, String input) {
        List<String> out = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream(field, input)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                out.add(term.toString());
            }
            stream.end();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    private static List<String> symbols(String input) {
        return tokens(MusicAnalyzer.symbols(), IndexFields.SYMBOL, input);
    }

    private static List<String> prose(String input) {
        return tokens(MusicAnalyzer.prose(), IndexFields.CONTENT, input);
    }

    @Test
    @DisplayName("an applied dominant survives tokenization as one symbol, plus its parts")
    void keepsAppliedDominantsWhole() {
        assertEquals(List.of("V7/V", "V7", "V"), symbols("V7/V"));
        assertEquals(List.of("V/V", "V", "V"), symbols("V/V"));
        assertNotEquals(symbols("V/V"), symbols("V7/V"));
    }

    @Test
    @DisplayName("case is meaning: V is a major chord and v is a minor one")
    void doesNotLowercaseRomanNumerals() {
        assertEquals(List.of("V"), symbols("V"));
        assertEquals(List.of("v"), symbols("v"));
        assertNotEquals(symbols("V"), symbols("v"));
    }

    @Test
    @DisplayName("half-diminished and fully diminished do not collapse into each other")
    void keepsDiminishedSignsApart() {
        assertEquals(List.of("iiø7"), symbols("iiø7"));
        assertEquals(List.of("vii°7"), symbols("vii°7"));
        assertNotEquals(symbols("iiø7"), symbols("vii°7"));
    }

    @Test
    void keepsAugmentedSixthsAndTheNeapolitan() {
        assertEquals(List.of("It+6"), symbols("It+6"));
        assertEquals(List.of("Fr+6"), symbols("Fr+6"));
        assertEquals(List.of("Ger+6"), symbols("Ger+6"));
        assertEquals(List.of("N6"), symbols("N6"));
        assertEquals(List.of("bII6"), symbols("bII6"));
        assertEquals(3, List.of(symbols("It+6"), symbols("Fr+6"), symbols("Ger+6")).stream().distinct().count());
    }

    @Test
    @DisplayName("a progression is one searchable thing as well as its chords")
    void keepsProgressionChainsWhole() {
        assertEquals(List.of("ii-V-I", "ii", "V", "I"), symbols("ii-V-I"));
    }

    @Test
    @DisplayName("publishers write flats and sharps as signs, and they must match the ASCII form")
    void normalisesUnicodeAccidentals() {
        assertEquals(symbols("bII6"), symbols("♭II6"));
        assertEquals(symbols("ii-V-I"), symbols("ii–V–I"));
    }

    @Test
    void dropsOrdinaryWordsFromTheSymbolField() {
        assertEquals(List.of(), symbols("the dominant resolves upward"));
    }

    @Test
    @DisplayName("A is a note and i is a roman numeral, so neither is a stop word")
    void keepsMusicallyMeaningfulStopWords() {
        assertTrue(prose("the note A above middle C").contains("a"));
        assertTrue(prose("the chord i in a minor key").contains("i"));
        assertFalse(prose("the note A").contains("the"));
    }

    @Test
    @DisplayName("stemming keeps real words, so cadences finds cadence")
    void stemsLightly() {
        assertEquals(prose("cadence"), prose("cadences"));
        assertTrue(prose("cadences").contains("cadence"));
    }
}
