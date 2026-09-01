package fr.lapetina.music.knowledge.harmony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Engraving real bars from a corpus note table.
 *
 * <p>The fixture is two bars of Beethoven's second piano sonata as the corpus publishes
 * them: two staves, two voices each.
 */
class ScoreExcerptWriterTest {

    private static NoteEvent note(int measure, double onset, double duration, int staff, int voice, String name) {
        return new NoteEvent(measure, onset, duration, staff, voice, name, false);
    }

    private static final List<NoteEvent> BAR_17 = List.of(
            note(17, 0.0, 0.125, 1, 1, "G#4"),
            note(17, 0.125, 0.125, 1, 1, "E4"),
            note(17, 0.0, 0.25, 1, 2, "B3"),
            note(17, 0.25, 0.25, 1, 2, "A3"),
            note(17, 0.25, 0.25, 1, 2, "D#4"),
            note(17, 0.0, 0.125, 2, 1, "E3"),
            note(17, 0.125, 0.125, 2, 1, "B2"),
            note(17, 0.0, 0.5, 2, 2, "E2"));

    private static String abc() {
        return ScoreExcerptWriter.toAbc(BAR_17, "Sonata no. 2", "A", "2/4", 17, 17, null);
    }

    @Test
    @DisplayName("a piano is engraved on two staves, not one staff per voice")
    void groupsVoicesOntoTheStaffTheyBelongTo() {
        assertEquals("(S1V1 S1V2) (S2V1 S2V2)",
                ScoreExcerptWriter.staffGrouping(List.of("S1V1", "S1V2", "S2V1", "S2V2")));
        assertTrue(abc().contains("%%score (S1V1 S1V2) (S2V1 S2V2)"), abc());
    }

    @Test
    @DisplayName("the clef is decided per staff, so an inner bass voice does not tear the staff apart")
    void usesOneClefPerStaff() {
        String abc = abc();
        assertTrue(abc.contains("V:S1V1 clef=treble"), abc);
        assertTrue(abc.contains("V:S1V2 clef=treble"), abc);
        assertTrue(abc.contains("V:S2V1 clef=bass"), abc);
        assertTrue(abc.contains("V:S2V2 clef=bass"), abc);
    }

    @Test
    @DisplayName("spelling survives: the sharp that makes this an applied dominant is written")
    void keepsTheSpelling() {
        String abc = abc();
        assertTrue(abc.contains("^G"), "G sharp, not A flat: " + abc);
        assertTrue(abc.contains("^D"), "D sharp, the third of V7/V: " + abc);
        assertFalse(abc.contains("_A"), abc);
    }

    @Test
    void writesTheKeyAndMetreItWasGiven() {
        String abc = abc();
        assertTrue(abc.contains("K:A"), abc);
        assertTrue(abc.contains("M:2/4"), abc);
        assertTrue(abc.contains("L:1/8"), abc);
    }

    @Test
    @DisplayName("notes starting together in one voice become a chord")
    void writesSimultaneousNotesAsAChord() {
        assertTrue(abc().contains("[A,^D]"), abc());
    }

    @Test
    @DisplayName("a sixteenth is engraved as a sixteenth, not as a sixty-fourth")
    void writesDurationsCorrectly() {
        // ABC reads n/d as the L: unit times n over d, and the unit here is an eighth.
        assertEquals("", ScoreExcerptWriter.length(1.0 / 8), "an eighth is the unit");
        assertEquals("2", ScoreExcerptWriter.length(1.0 / 4), "a quarter is two eighths");
        assertEquals("4", ScoreExcerptWriter.length(1.0 / 2));
        assertEquals("8", ScoreExcerptWriter.length(1.0));
        assertEquals("/2", ScoreExcerptWriter.length(1.0 / 16), "a sixteenth is half an eighth");
        assertEquals("/4", ScoreExcerptWriter.length(1.0 / 32));
        assertEquals("3/2", ScoreExcerptWriter.length(3.0 / 16), "a dotted eighth");
        assertEquals("3", ScoreExcerptWriter.length(3.0 / 8), "a dotted quarter");
        assertEquals("/3", ScoreExcerptWriter.length(1.0 / 24), "an eighth-note triplet");
    }

    @Test
    @DisplayName("a bar of sixteenths comes out as sixteenths")
    void keepsTheRhythmOfARealBar() {
        List<NoteEvent> semiquavers = List.of(
                note(1, 0.0, 0.0625, 1, 1, "C4"),
                note(1, 0.0625, 0.0625, 1, 1, "D4"),
                note(1, 0.125, 0.0625, 1, 1, "E4"),
                note(1, 0.1875, 0.0625, 1, 1, "F4"));

        String abc = ScoreExcerptWriter.toAbc(semiquavers, null, "C", "2/4", 1, 1, null);

        assertTrue(abc.contains("C/2D/2E/2F/2"), abc);
        // The header legitimately says L:1/8; the notes must not.
        String notes = abc.substring(abc.indexOf("clef=treble"));
        assertFalse(notes.contains("/8"), "a sixteenth written as /8 would be four times too short");
    }

    @Test
    void producesNothingFromNothing() {
        assertEquals("", ScoreExcerptWriter.toAbc(List.of(), "x", "C", "4/4", 1, 1, null));
    }

    @Test
    @DisplayName("the metre comes from the source, so a piece in 3/4 is not misbarred as 4/4")
    void takesTheMetreFromTheNotes() {
        List<NoteEvent> inThree = List.of(
                new NoteEvent(1, 0.0, 0.25, 1, 1, "C4", false, "3/4"),
                new NoteEvent(1, 0.25, 0.25, 1, 1, "D4", false, "3/4"));

        assertTrue(ScoreExcerptWriter.toAbc(inThree, null, "C", null, 1, 1, null).contains("M:3/4"));
        assertEquals("3/4", ScoreExcerptWriter.metreOf(inThree, "4/4"));
        assertEquals("4/4", ScoreExcerptWriter.metreOf(List.of(), null), "a sane default, not a guess");
    }

    @Test
    @DisplayName("the harmony being taught is annotated over the bar it happens in")
    void annotatesTheTargetHarmony() {
        String abc = ScoreExcerptWriter.toAbc(BAR_17, null, "A", "2/4", 17, 17,
                new ScoreExcerptWriter.Target(17, 0.0, "V7/V"));

        // The caret matters: without it ABC reads V7/V as a slash chord and draws "V7".
        assertTrue(abc.contains("\"^V7/V\""), abc);
        assertEquals(1, abc.split("\"\\^V7/V\"", -1).length - 1,
                "annotated once, on one staff, not above every voice");
    }

    @Test
    @DisplayName("an annotation is presentation: the notes are unchanged by it")
    void doesNotAlterTheMusicToAnnotateIt() {
        String plain = ScoreExcerptWriter.toAbc(BAR_17, null, "A", "2/4", 17, 17, null);
        String marked = ScoreExcerptWriter.toAbc(BAR_17, null, "A", "2/4", 17, 17,
                new ScoreExcerptWriter.Target(17, 0.0, "V7/V"));

        assertEquals(plain.replace("|]", ""), marked.replace("\"^V7/V\"", "").replace("|]", ""));
    }
}
