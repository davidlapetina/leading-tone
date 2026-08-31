package fr.lapetina.music.theoryservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.theory.Cadence;
import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.Interval;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.RomanNumeralAnalysis;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The façade, which is the only way the rest of the application reaches the theory engine.
 * Plain unit test: it holds no state and needs no container.
 */
class MusicTheoryServiceTest {

    private final MusicTheoryService theory = new MusicTheoryService();

    private static List<String> names(List<PitchClass> pitchClasses) {
        return pitchClasses.stream().map(PitchClass::name).toList();
    }

    @Test
    @DisplayName("the four key parsers this replaced all accepted different things; this accepts all of them")
    void readsKeysWrittenAnyOfTheWaysThisApplicationWritesThem() {
        assertEquals("C major", theory.parseKey("C").name());
        assertEquals("C major", theory.parseKey("C major").name());
        assertEquals("F# minor", theory.parseKey("f# minor").name());
        assertEquals("Bb minor", theory.parseKey("Bbm").name());
        assertEquals("Eb major", theory.parseKey("Eb_major").name());
        assertEquals("C major", theory.parseKey("C ionian").name(),
                "an unrecognised mode word reads as major rather than throwing at the model");
        assertTrue(theory.tryParseKey("not a key at all").isEmpty());
    }

    @Test
    void identifiesAndBuildsIntervals() {
        assertEquals(Interval.MAJOR_THIRD, theory.identifyInterval("C", "E"));
        assertEquals(Interval.MINOR_THIRD, theory.identifyInterval("C", "Eb"));
        assertEquals(Interval.AUGMENTED_FOURTH, theory.identifyInterval("C", "F#"));
        assertEquals("Ab", theory.buildInterval("C", "m6").name());
        assertEquals("A", theory.buildInterval("C#", "m6").name());
    }

    @Test
    @DisplayName("what is V7/V in C major")
    void answersTheQuestionDeterministically() {
        RomanNumeralAnalysis analysis = theory.realizeRomanNumeral("V7/V", "C major");

        assertEquals(List.of("D", "F#", "A", "C"), names(analysis.pitchClasses()));
        assertEquals("D7", analysis.chord().symbol());
        assertEquals(5, analysis.targetDegree());
    }

    @Test
    void buildsScalesAndChords() {
        assertEquals(List.of("C", "D", "E", "F", "G", "A", "B"),
                names(theory.buildScale("C", "major").pitchClasses()));
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G#"),
                names(theory.buildScale("A", "harmonic_minor").pitchClasses()));
        assertEquals(List.of("G", "B", "D", "F", "Ab"),
                names(theory.buildChord("G7(b9)").pitchClasses()));
    }

    @Test
    void transposesAProgression() {
        assertEquals(List.of("Em7", "A7", "Dmaj7"),
                theory.transposeProgression("ii7 V7 Imaj7", "C major", "D major")
                        .stream().map(Chord::symbol).toList());
    }

    @Test
    @DisplayName("a cadence is only claimed when the ending supports the claim")
    void identifiesCadencesConservatively() {
        assertEquals(Cadence.PERFECT_AUTHENTIC, theory.identifyCadence(List.of("F", "G", "C"), "C major"));
        assertEquals(Cadence.NONE, theory.identifyCadence(List.of("C", "Ebm"), "C major"));
        assertEquals(Cadence.NONE, theory.identifyCadence(List.of("C"), "C major"));
    }
}
