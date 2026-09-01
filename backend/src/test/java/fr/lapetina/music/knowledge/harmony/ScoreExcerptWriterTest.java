package fr.lapetina.music.knowledge.harmony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
    @DisplayName("a bar's rest is as long as the bar, whatever the metre")
    void restsFillExactlyTheBar() {
        assertEquals(8, ScoreExcerptWriter.unitsPerBar("4/4"));
        assertEquals(4, ScoreExcerptWriter.unitsPerBar("2/4"), "a 2/4 bar is four eighths");
        assertEquals(6, ScoreExcerptWriter.unitsPerBar("3/4"));
        assertEquals(6, ScoreExcerptWriter.unitsPerBar("6/8"));
        assertEquals(8, ScoreExcerptWriter.unitsPerBar(null), "a sane default, not a guess");
        assertEquals(8, ScoreExcerptWriter.unitsPerBar("nonsense"));

        // A voice silent through the second bar of a 2/4 excerpt.
        List<NoteEvent> onlyInBarOne = List.of(
                new NoteEvent(1, 0.0, 0.25, 1, 1, "C4", false, "2/4"),
                new NoteEvent(1, 0.25, 0.25, 1, 1, "D4", false, "2/4"));

        String abc = ScoreExcerptWriter.toAbc(onlyInBarOne, null, "C", null, 1, 2, null);

        assertTrue(abc.contains("z4"), "an empty 2/4 bar rests for four eighths: " + abc);
        assertFalse(abc.contains("z8"), "z8 would be twice the bar");
    }

    @Test
    @DisplayName("three notes in the time of two are engraved as a triplet, not as dotted notes")
    void groupsTriplets() {
        // Eighth-note triplets: three notes of a twelfth of a whole note each.
        double twelfth = 1.0 / 12;
        List<NoteEvent> triplet = List.of(
                note(1, 0.0, twelfth, 1, 1, "G3"),
                note(1, twelfth, twelfth, 1, 1, "C4"),
                note(1, 2 * twelfth, twelfth, 1, 1, "G4"));

        String abc = ScoreExcerptWriter.toAbc(triplet, null, "C", "2/4", 1, 1, null);

        // (p:q:r — three in the time of two, for three notes. Written out rather than left to
        // ABC's bare (3, whose meaning for other tuplet sizes depends on the metre.
        assertTrue(abc.contains("(3:2:3G,CG"), "expected a triplet group: " + abc);
        assertFalse(abc.contains("2/3"),
                "a two-thirds length is arithmetically right and engraves as a dotted note");
    }

    @Test
    @DisplayName("ordinary rhythms are left alone")
    void doesNotInventTriplets() {
        List<NoteEvent> plain = List.of(
                note(1, 0.0, 0.125, 1, 1, "C4"),
                note(1, 0.125, 0.125, 1, 1, "D4"),
                note(1, 0.25, 0.125, 1, 1, "E4"));

        assertFalse(ScoreExcerptWriter.toAbc(plain, null, "C", "2/4", 1, 1, null).contains("(3"));
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

    @Test
    @DisplayName("every bar it writes is exactly as long as the metre says")
    void barsAreAlwaysTheRightLength() {
        // The invariant that matters most, and the one that is invisible in a passing test:
        // a bar that is short or long engraves every bar after it in the wrong place. These
        // are the shapes that broke it -- a voice falling silent, a note held over the
        // barline, an ornament with no duration, and tuplets that are not thirds.
        record Case(String name, String metre, List<NoteEvent> notes) {}
        List<Case> cases = List.of(
                new Case("a voice that stops early", "2/4",
                        List.of(note(1, 0.0, 0.125, 1, 1, "C4"))),
                new Case("a note held across the barline", "2/4",
                        List.of(note(1, 0.25, 0.75, 1, 1, "C4"))),
                new Case("a grace note, which has no duration", "3/4",
                        List.of(note(1, 0.0, 0.0, 1, 1, "B4"),
                                note(1, 0.0, 0.75, 1, 1, "C5"))),
                new Case("a quintuplet", "2/4",
                        List.of(note(1, 0.0, 0.025, 1, 1, "C4"),
                                note(1, 0.025, 0.025, 1, 1, "D4"),
                                note(1, 0.05, 0.025, 1, 1, "E4"),
                                note(1, 0.075, 0.025, 1, 1, "F4"),
                                note(1, 0.1, 0.025, 1, 1, "G4"),
                                note(1, 0.125, 0.375, 1, 1, "C5"))),
                new Case("a bar of rests", "4/4", List.of(note(1, 0.0, 1.0, 1, 1, "C4"))));

        for (Case each : cases) {
            String abc = ScoreExcerptWriter.toAbc(each.notes(), null, "C", each.metre(), 1, 1, null);
            for (String bar : musicBarsOf(abc)) {
                assertEquals(unitsPerBar(each.metre()), unitsIn(bar), 1e-6,
                        each.name() + " should fill its bar exactly: " + abc);
            }
        }
    }

    @Test
    @DisplayName("a minor key is written with an upper-case tonic, or the key signature is dropped")
    void capitalisesTheKey() {
        List<NoteEvent> notes = List.of(note(1, 0.0, 1.0, 1, 1, "C4"));

        assertTrue(ScoreExcerptWriter.toAbc(notes, null, "fm", "4/4", 1, 1, null).contains("K:Fm"),
                "K:fm is a parse error, and the staff then has no accidentals at all");
        assertTrue(ScoreExcerptWriter.toAbc(notes, null, "c#m", "4/4", 1, 1, null).contains("K:C#m"),
                "an accidental in the tonic must survive capitalisation");
        assertTrue(ScoreExcerptWriter.toAbc(notes, null, "eb", "4/4", 1, 1, null).contains("K:Ebm"),
                "the corpora write minor in lower case, so the case carries the mode too");
        assertTrue(ScoreExcerptWriter.toAbc(notes, null, "Bb", "4/4", 1, 1, null).contains("K:Bb"),
                "a major key is already correct and must be left alone");
    }

    @Test
    @DisplayName("a long-short triplet is one tuplet, not two notes with impossible lengths")
    void groupsUnequalTuplets() {
        // A quarter and an eighth in the time of two eighths: the members are not equal, and
        // reading them as anything but a triplet writes a rhythm the composer did not.
        double third = 1.0 / 12;
        List<NoteEvent> longShort = List.of(
                note(1, 0.0, 2 * third, 1, 1, "C4"),
                note(1, 2 * third, third, 1, 1, "D4"));

        String abc = ScoreExcerptWriter.toAbc(longShort, null, "C", "2/4", 1, 1, null);

        assertTrue(abc.contains("(3:2:2"), "three in the time of two, for two notes: " + abc);
        assertFalse(abc.contains("/12"), "a twelfth is not a note value: " + abc);
    }

    @Test
    @DisplayName("a rhythm with no notation is left without a score rather than approximated")
    void refusesToApproximateAnUnwritableRhythm() {
        // A lone seventh of a whole note. As one of seven it would be a septuplet, but on its
        // own no tuplet explains it and no sum of note values reaches it, and rounding it
        // would print a rhythm nobody wrote.
        List<NoteEvent> unwritable = List.of(note(1, 0.0, 1.0 / 7, 1, 1, "C4"));

        assertEquals("", ScoreExcerptWriter.toAbc(unwritable, null, "C", "2/4", 1, 1, null),
                "an excerpt that cannot be engraved honestly keeps its citation and loses its score");
    }

    private static double unitsPerBar(String metre) {
        String[] parts = metre.split("/");
        return Integer.parseInt(parts[0]) * 8.0 / Integer.parseInt(parts[1]);
    }

    /** The bars of the music lines, ignoring the ABC header and any trailing double bar. */
    private static List<String> musicBarsOf(String abc) {
        List<String> bars = new ArrayList<>();
        for (String line : abc.split("\\n")) {
            if (line.isBlank() || (line.length() > 1 && line.charAt(1) == ':') || line.startsWith("%")) {
                continue;
            }
            for (String bar : line.replace("|]", "|").split("\\|")) {
                if (!bar.isBlank()) {
                    bars.add(bar);
                }
            }
        }
        return bars;
    }

    /**
     * The length of one bar in eighth-note units, read back out of the ABC.
     *
     * <p>Deliberately a separate reading of the notation rather than a call back into the
     * writer: a bug shared by both would cancel out and the test would pass regardless.
     */
    private static double unitsIn(String bar) {
        double total = 0;
        int i = 0;
        int tupletLeft = 0;
        double tupletRatio = 1;
        while (i < bar.length()) {
            char c = bar.charAt(i);
            if (c == '"') {                       // a chord symbol or annotation
                i = bar.indexOf('"', i + 1) + 1;
            } else if (c == '{') {                // grace notes, which have no duration
                i = bar.indexOf('}', i) + 1;
            } else if (c == '(' && i + 1 < bar.length() && Character.isDigit(bar.charAt(i + 1))) {
                java.util.regex.Matcher tuplet = java.util.regex.Pattern
                        .compile("\\((\\d+):(\\d+):(\\d+)").matcher(bar.substring(i));
                assertTrue(tuplet.lookingAt(), "tuplets are written in full (p:q:r): " + bar);
                tupletRatio = Integer.parseInt(tuplet.group(2)) / (double) Integer.parseInt(tuplet.group(1));
                tupletLeft = Integer.parseInt(tuplet.group(3));
                i += tuplet.end();
            } else if (c == '[' || Character.isLetter(c) || c == '^' || c == '_' || c == '=') {
                int start = i;
                if (c == '[') {                   // a chord sounds once, however many notes
                    i = bar.indexOf(']', i) + 1;
                } else {
                    while (i < bar.length() && "^_=".indexOf(bar.charAt(i)) >= 0) {
                        i++;
                    }
                    i++;                          // the note letter
                }
                while (i < bar.length() && ",'".indexOf(bar.charAt(i)) >= 0) {
                    i++;                          // octave marks
                }
                int lengthAt = i;
                while (i < bar.length() && (Character.isDigit(bar.charAt(i)) || bar.charAt(i) == '/')) {
                    i++;
                }
                double units = lengthOf(bar.substring(lengthAt, i));
                assertTrue(start < i, "made no progress reading: " + bar);
                total += tupletLeft > 0 ? units * tupletRatio : units;
                if (tupletLeft > 0) {
                    tupletLeft--;
                }
            } else {
                i++;                              // ties, spaces, anything without duration
            }
        }
        return total;
    }

    /** An ABC length suffix: "" is one unit, "3" is three, "/4" is a quarter, "3/2" is one and a half. */
    private static double lengthOf(String suffix) {
        if (suffix.isEmpty()) {
            return 1;
        }
        if (suffix.equals("/")) {
            return 0.5;
        }
        int slash = suffix.indexOf('/');
        if (slash < 0) {
            return Integer.parseInt(suffix);
        }
        double numerator = slash == 0 ? 1 : Integer.parseInt(suffix.substring(0, slash));
        String denominator = suffix.substring(slash + 1);
        return numerator / (denominator.isEmpty() ? 2.0 : Integer.parseInt(denominator));
    }
}
