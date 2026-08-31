package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CounterpointAnalyzerTest {

    private static Note n(String name) {
        return Note.parse(name);
    }

    @Test
    void namesHowTwoVoicesMove() {
        // C-G rising to D-A: same direction, same fifth between them.
        assertEquals(Motion.PARALLEL,
                CounterpointAnalyzer.motionBetween(n("C4"), n("G4"), n("D4"), n("A4")));
        // Both rise, but the gap widens.
        assertEquals(Motion.SIMILAR,
                CounterpointAnalyzer.motionBetween(n("C4"), n("E4"), n("D4"), n("B4")));
        // Voices move apart.
        assertEquals(Motion.CONTRARY,
                CounterpointAnalyzer.motionBetween(n("C4"), n("E4"), n("B3"), n("G4")));
        // One holds.
        assertEquals(Motion.OBLIQUE,
                CounterpointAnalyzer.motionBetween(n("C4"), n("E4"), n("C4"), n("G4")));
        assertEquals(Motion.STATIC,
                CounterpointAnalyzer.motionBetween(n("C4"), n("E4"), n("C4"), n("E4")));
    }

    @Test
    @DisplayName("parallel fifths and octaves are caught; parallel thirds are not")
    void catchesTheForbiddenParallels() {
        assertTrue(CounterpointAnalyzer.hasParallelPerfects(n("C4"), n("G4"), n("D4"), n("A4")));
        assertTrue(CounterpointAnalyzer.hasParallelPerfects(n("C4"), n("C5"), n("D4"), n("D5")));
        // Parallel thirds are ordinary and allowed.
        assertFalse(CounterpointAnalyzer.hasParallelPerfects(n("C4"), n("E4"), n("D4"), n("F4")));
        // Both voices descending from a fifth to a fifth is still parallel, whatever the
        // direction: this is the case that catches people out.
        assertTrue(CounterpointAnalyzer.hasParallelPerfects(n("C4"), n("G4"), n("B3"), n("F#4")));
        // A fifth *arrived at* by contrary motion is fine.
        assertFalse(CounterpointAnalyzer.hasParallelPerfects(n("C4"), n("E4"), n("B3"), n("F#4")));
    }

    @Test
    void classifiesConsonanceTheWayFirstSpeciesDoes() {
        assertTrue(CounterpointAnalyzer.isConsonant(Interval.MAJOR_THIRD));
        assertTrue(CounterpointAnalyzer.isConsonant(Interval.MINOR_SIXTH));
        assertTrue(CounterpointAnalyzer.isConsonant(Interval.PERFECT_FIFTH));
        assertFalse(CounterpointAnalyzer.isConsonant(Interval.MAJOR_SECOND));
        assertFalse(CounterpointAnalyzer.isConsonant(Interval.MINOR_SEVENTH));
        assertFalse(CounterpointAnalyzer.isConsonant(Interval.AUGMENTED_FOURTH));
    }

    @Test
    void reportsWhereAPassageFirstGoesWrong() {
        List<Note> lower = List.of(n("C4"), n("D4"), n("E4"));
        List<Note> upper = List.of(n("E4"), n("A4"), n("B4"));
        assertEquals(2, CounterpointAnalyzer.firstParallelPerfect(lower, upper));

        List<Note> clean = List.of(n("C4"), n("D4"), n("E4"));
        List<Note> against = List.of(n("E4"), n("F4"), n("G4"));
        assertEquals(-1, CounterpointAnalyzer.firstParallelPerfect(clean, against));
    }
}
