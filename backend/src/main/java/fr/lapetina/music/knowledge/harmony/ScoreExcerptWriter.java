package fr.lapetina.music.knowledge.harmony;

import fr.lapetina.music.theory.Note;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Turns published note tables into ABC notation.
 *
 * <p>Written as one ABC voice per staff-and-voice the corpus records, so what is engraved
 * is what the corpus says is there, rather than a reduction dressed up as a score. Where a
 * voice has a gap, a rest is written; where several notes start together in one voice, they
 * are engraved as a chord.
 *
 * <p>What this does <em>not</em> claim to reproduce is the engraving itself: beaming,
 * slurs, articulation, pedalling and layout are not in the data being read and are not
 * invented here. It is the notes, in the right bars, at the right time.
 */
public final class ScoreExcerptWriter {

    /** Eighth notes, which keeps most classical piano writing to whole numbers. */
    private static final double UNIT = 0.125;

    private ScoreExcerptWriter() {}

    /**
     * @param target the measure whose harmony is being taught, annotated above the staff so
     *     the eye lands on it, or null for no annotation
     */
    public static String toAbc(List<NoteEvent> notes, String title, String keySignature,
                               String timeSignature, int fromMeasure, int toMeasure,
                               Target target) {
        try {
            return write(notes, title, keySignature, timeSignature, fromMeasure, toMeasure, target);
        } catch (Unwritable unwritable) {
            // The rhythm cannot be notated exactly. Showing an approximation would print
            // something the composer did not write, so the excerpt keeps its citation and
            // loses its score -- a gap the reader can see, rather than a quiet mistake.
            return "";
        }
    }

    private static String write(List<NoteEvent> notes, String title, String keySignature,
                                String timeSignature, int fromMeasure, int toMeasure,
                                Target target) {
        if (notes.isEmpty()) {
            return "";
        }
        Map<String, List<NoteEvent>> voices = byVoice(notes);
        StringBuilder abc = new StringBuilder();
        abc.append("X:1\n");
        if (title != null && !title.isBlank()) {
            abc.append("T:").append(title).append('\n');
        }
        // The metre comes from the notes themselves. Assuming 4/4 silently misbars a piece
        // in 3/4 or 6/8, which makes an excerpt look wrong to anybody who can read it.
        String metre = metreOf(notes, timeSignature);
        abc.append("M:").append(metre).append('\n');
        abc.append("L:1/8\n");
        if (voices.size() > 1) {
            // Voices are bracketed by the staff they belong to, so a piano is engraved on
            // two staves with two voices each. Listing them flat makes abcjs draw one staff
            // per voice, which is four staves for a piano and not what the music looks like.
            abc.append("%%score ").append(staffGrouping(voices.keySet())).append('\n');
        }
        abc.append("K:").append(keyField(keySignature)).append('\n');

        Map<String, String> clefs = clefsByStaff(voices);
        for (Map.Entry<String, List<NoteEvent>> voice : voices.entrySet()) {
            abc.append("V:").append(voice.getKey())
                    .append(clefs.get(voice.getKey().substring(0, voice.getKey().indexOf('V'))))
                    .append('\n');
            abc.append(writeVoice(voice.getValue(), fromMeasure, toMeasure, metre,
                    firstVoice(voices, voice.getKey()) ? target : null)).append("|]\n");
        }
        return abc.toString();
    }

    /**
     * Groups voice names by staff: {@code (S1V1 S1V2) (S2V1 S2V2)}. The brackets are what
     * tell the engraver that two voices share one staff.
     */
    static String staffGrouping(java.util.Collection<String> voiceNames) {
        Map<String, List<String>> byStaff = new LinkedHashMap<>();
        for (String voice : voiceNames) {
            byStaff.computeIfAbsent(voice.substring(0, voice.indexOf('V')), key -> new ArrayList<>()).add(voice);
        }
        StringBuilder grouping = new StringBuilder();
        for (List<String> staff : byStaff.values()) {
            grouping.append(grouping.isEmpty() ? "" : " ")
                    .append(staff.size() == 1 ? staff.get(0) : "(" + String.join(" ", staff) + ")");
        }
        return grouping.toString();
    }

    /** How many eighth-note units a bar of this metre holds. */
    static int unitsPerBar(String metre) {
        if (metre == null || !metre.contains("/")) {
            return 8;
        }
        try {
            int slash = metre.indexOf('/');
            int beats = Integer.parseInt(metre.substring(0, slash).trim());
            int unit = Integer.parseInt(metre.substring(slash + 1).trim());
            int units = beats * 8 / unit;
            return units > 0 ? units : 8;
        } catch (NumberFormatException notAMetre) {
            return 8;
        }
    }

    /** What is being pointed at, and what to call it. */
    public record Target(int measure, Double beat, String label) {}

    /** The metre the corpus recorded, preferring what the notes themselves carry. */
    static String metreOf(List<NoteEvent> notes, String fallback) {
        return notes.stream()
                .map(NoteEvent::timeSignature)
                .filter(signature -> signature != null && !signature.isBlank())
                .findFirst()
                .orElse(fallback == null || fallback.isBlank() ? "4/4" : fallback);
    }

    /** The annotation belongs on one staff, not repeated above every voice. */
    private static boolean firstVoice(Map<String, List<NoteEvent>> voices, String name) {
        return voices.keySet().iterator().next().equals(name);
    }

    /** Ordered so the treble staff is written first, as it is read. */
    private static Map<String, List<NoteEvent>> byVoice(List<NoteEvent> notes) {
        Map<String, List<NoteEvent>> voices = new LinkedHashMap<>();
        notes.stream()
                .sorted(Comparator.comparingInt(NoteEvent::staff).thenComparingInt(NoteEvent::voice))
                .forEach(note -> voices.computeIfAbsent("S" + note.staff() + "V" + note.voice(),
                        key -> new ArrayList<>()).add(note));
        return voices;
    }

    /**
     * The ABC key field, whose tonic has to be upper case.
     *
     * <p>{@code K:fm} is not a key signature but a parse error, and a reader that rejects the
     * field draws the staff with no accidentals at all -- so an F minor excerpt appears in C
     * major and every flat in the music looks like an editorial accidental. Lower case is also
     * how the corpora write minor, so it is the mode as well as a mistake.
     */
    private static String keyField(String keySignature) {
        if (keySignature == null || keySignature.isBlank()) {
            return "C";
        }
        String key = keySignature.trim();
        boolean minor = Character.isLowerCase(key.charAt(0));
        String tonic = Character.toUpperCase(key.charAt(0)) + key.substring(1);
        return minor && !tonic.toLowerCase().endsWith("m") ? tonic + "m" : tonic;
    }

    /**
     * One clef per staff, decided from everything on it. Deciding per voice puts an inner
     * bass-staff voice on a treble clef and tears the staff in half.
     */
    private static Map<String, String> clefsByStaff(Map<String, List<NoteEvent>> voices) {
        Map<String, List<NoteEvent>> byStaff = new LinkedHashMap<>();
        voices.forEach((name, notes) ->
                byStaff.computeIfAbsent(name.substring(0, name.indexOf('V')), key -> new ArrayList<>())
                        .addAll(notes));
        Map<String, String> clefs = new LinkedHashMap<>();
        byStaff.forEach((staff, notes) -> clefs.put(staff, clefFor(notes)));
        return clefs;
    }

    private static String clefFor(List<NoteEvent> notes) {
        double average = notes.stream()
                .map(note -> Note.parse(note.name()))
                .mapToInt(Note::midi)
                .average()
                .orElse(60);
        return average < 58 ? " clef=bass" : " clef=treble";
    }

    private static String writeVoice(List<NoteEvent> notes, int fromMeasure, int toMeasure,
                                     String metre, Target target) {
        StringBuilder body = new StringBuilder();
        for (int measure = fromMeasure; measure <= toMeasure; measure++) {
            if (target != null && target.measure() == measure && target.label() != null) {
                // An annotation, not a chord symbol. Without the leading caret, ABC reads
                // "V7/V" as a slash chord and engraves it as "V7" -- a different chord from
                // the one being taught. The caret means "this text, above the staff".
                body.append('"').append('^').append(target.label().replace("\"", "")).append('"');
            }
            body.append(writeMeasure(notes, measure, metre));
            if (measure < toMeasure) {
                body.append('|');
            }
        }
        return body.toString();
    }

    private static String writeMeasure(List<NoteEvent> notes, int measure, String metre) {
        List<NoteEvent> inBar = notes.stream().filter(note -> note.measure() == measure).toList();
        if (inBar.isEmpty()) {
            // A bar's rest is as long as the bar. Assuming eight eighths writes a whole note
            // into a 2/4 bar, which is twice the music.
            return "z" + unitsPerBar(metre);
        }
        // Grace notes are published with a duration of zero. Left in the arithmetic they make
        // the shortest note at their onset zero, and the bar collapses; dropped, the music
        // loses its ornaments. They are written as grace notes, which is what they are.
        List<NoteEvent> sounding = inBar.stream().filter(note -> note.duration() > 0).toList();
        List<NoteEvent> graces = inBar.stream().filter(note -> note.duration() <= 0).toList();
        if (sounding.isEmpty()) {
            return "z" + unitsPerBar(metre);
        }

        TreeSet<Double> onsets = new TreeSet<>();
        sounding.forEach(note -> onsets.add(note.onset()));

        // Built as events first, so runs of three can be recognised as tuplets afterwards.
        // Written straight out, a triplet eighth becomes a two-thirds length that engravers
        // draw as a dotted note -- a different rhythm from the one in the score.
        // A bar has a length, and everything written into it has to add up to that length.
        // A voice that falls silent before the end needs the rest written out, and a note
        // that is held across the barline has to stop at it -- otherwise the bar is short or
        // long, and every bar after it is drawn in the wrong place.
        double barLength = unitsPerBar(metre) * UNIT;

        List<Event> events = new ArrayList<>();
        double cursor = 0.0;
        List<Double> ordered = new ArrayList<>(onsets);
        for (int i = 0; i < ordered.size(); i++) {
            double onset = ordered.get(i);
            if (onset >= barLength - 1e-9) {
                break;      // published past the end of the bar; not ours to draw
            }
            if (onset > cursor + 1e-9) {
                events.add(new Event("z", (onset - cursor) / UNIT));
                cursor = onset;
            }
            List<NoteEvent> starting = sounding.stream()
                    .filter(note -> Math.abs(note.onset() - onset) < 1e-9)
                    .toList();
            double shortest = starting.stream().mapToDouble(NoteEvent::duration).min().orElse(UNIT);
            double untilNext = i + 1 < ordered.size() ? ordered.get(i + 1) - onset : shortest;
            double written = Math.min(shortest, untilNext <= 0 ? shortest : untilNext);
            written = Math.min(written, barLength - onset);
            if (written <= 0) {
                continue;
            }
            List<NoteEvent> ornaments = graces.stream()
                    .filter(note -> Math.abs(note.onset() - onset) < 1e-9)
                    .toList();
            events.add(new Event(graceOf(ornaments) + chord(starting), written / UNIT));
            cursor = onset + written;
        }
        if (cursor < barLength - 1e-9) {
            events.add(new Event("z", (barLength - cursor) / UNIT));
        }
        return render(events, unitsPerBeat(metre));
    }

    /** One thing to write, and how long it lasts in eighth-note units. */
    private record Event(String text, double units) {}

    /**
     * Writes the bar, marking triplets as triplets.
     *
     * <p>Three equal notes filling the time of two are written {@code (3} followed by the
     * three at their notated value. Emitting them at two-thirds length instead is
     * arithmetically the same and engraves as dotted notes, which reads as a different
     * rhythm to anybody playing from it.
     */
    /**
     * Writes the events of one bar, beamed by beat.
     *
     * <p>ABC beams everything that is not separated by a space, so without one a bar of
     * sixteenths is drawn under a single beam running its whole width. Music is beamed in
     * beats, because that is what makes the metre readable at a glance.
     */
    private static String render(List<Event> events, double beatUnits) {
        StringBuilder bar = new StringBuilder();
        double position = 0;
        int i = 0;
        while (i < events.size()) {
            if (position > 0 && Math.abs(position % beatUnits) < 1e-6) {
                bar.append(' ');
            }
            Tuplet tuplet = tupletAt(events, i);
            if (tuplet != null) {
                // (p:q:r — p notes in the time of q, for the next r. Written out in full rather
                // than left to ABC's bare (3, whose meaning for other sizes depends on the metre.
                bar.append('(').append(tuplet.played()).append(':').append(tuplet.inTheTimeOf())
                        .append(':').append(tuplet.notes());
                for (int n = i; n < i + tuplet.notes(); n++) {
                    Event event = events.get(n);
                    writeEvent(bar, event.text(), event.units() * tuplet.scale());
                    position += event.units();
                }
                i += tuplet.notes();
                continue;
            }
            writeEvent(bar, events.get(i).text(), events.get(i).units());
            position += events.get(i).units();
            i++;
        }
        return bar.toString();
    }

    /**
     * The beat, in eighth-note units — the unit music is beamed in.
     *
     * <p>Compound metres are counted in dotted beats: 6/8 is two beats of three eighths, not
     * six of one, and beaming it in ones would make it look like 6/4.
     */
    private static double unitsPerBeat(String metre) {
        String[] parts = metre.split("/");
        int count = Integer.parseInt(parts[0]);
        int value = Integer.parseInt(parts[1]);
        if (value == 8 && count % 3 == 0 && count > 3) {
            return 3;
        }
        return value == 2 ? 4 : 2;
    }

    /** A run of notes written as one tuplet: {@code played} in the time of {@code inTheTimeOf}. */
    private record Tuplet(int played, int inTheTimeOf, int notes) {
        double scale() {
            return played / (double) inTheTimeOf;
        }
    }

    /**
     * Tuplet ratios worth recognising, in the order a copyist would reach for them.
     *
     * <p>Ordered so a run is read as the simplest thing that explains it: a sextuplet is not
     * two triplets, and a triplet is not the first three notes of a quintuplet.
     */
    private static final int[][] RATIOS = {{3, 2}, {6, 4}, {5, 4}, {7, 4}, {9, 8}, {5, 2}, {7, 2}};

    /**
     * The tuplet starting here, or null if this note stands on its own.
     *
     * <p>A tuplet is a run whose members have no note value of their own but whose total is an
     * ordinary one. The members need not be equal: a long-short triplet is two notes in the
     * time of two, and reading it as anything else writes a rhythm the composer did not.
     */
    private static Tuplet tupletAt(List<Event> events, int at) {
        if (events.get(at).units() <= 0 || isWritable(events.get(at).units())) {
            return null;
        }
        for (int[] ratio : RATIOS) {
            double scale = ratio[0] / (double) ratio[1];
            for (int notes = 2; notes <= Math.min(9, events.size() - at); notes++) {
                double total = 0;
                boolean writable = true;
                for (int n = at; n < at + notes; n++) {
                    total += events.get(n).units();
                    if (!isWritable(events.get(n).units() * scale)) {
                        writable = false;
                        break;
                    }
                }
                if (writable && isWritable(total)) {
                    return new Tuplet(ratio[0], ratio[1], notes);
                }
            }
        }
        return null;
    }

    /** The note lengths that exist as a single symbol: plain, dotted and double-dotted. */
    private static final double[] WRITABLE =
            {16, 14, 12, 8, 7, 6, 4, 3.5, 3, 2, 1.75, 1.5, 1, 0.875, 0.75, 0.5, 0.375, 0.25, 0.125};

    private static boolean isWritable(double units) {
        for (double value : WRITABLE) {
            if (Math.abs(units - value) < 1e-6) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes one event, splitting a duration that has no symbol of its own.
     *
     * <p>There is no five-eighth rest and no five-eighth note, and a reader given {@code z5}
     * cannot draw the bar at all. The length is written instead as the notes a copyist would
     * use: consecutive rests, or tied notes.
     *
     * @throws Unwritable if no combination of note values adds up to this duration, which
     *     means the excerpt cannot be engraved honestly and so is not engraved at all
     */
    private static void writeEvent(StringBuilder bar, String text, double units) {
        double left = units;
        boolean first = true;
        while (left > 1e-6) {
            double part = largestWritableWithin(left);
            if (part <= 0) {
                throw new Unwritable(units);
            }
            if (!first && !text.startsWith("z")) {
                bar.append('-');
            }
            bar.append(text).append(lengthOfUnits(part));
            left -= part;
            first = false;
        }
    }

    private static double largestWritableWithin(double units) {
        for (double value : WRITABLE) {
            if (value <= units + 1e-6) {
                return value;
            }
        }
        return 0;
    }

    /**
     * A duration that cannot be written as real note values.
     *
     * <p>Raised rather than approximated: rounding it would print a rhythm the composer did
     * not write, and the excerpt is shown without a score instead, which the reader can see.
     */
    static final class Unwritable extends RuntimeException {
        Unwritable(double units) {
            super("no combination of note values makes " + units + " eighths");
        }
    }

    /** ABC writes grace notes in braces before the note they decorate. */
    private static String graceOf(List<NoteEvent> ornaments) {
        if (ornaments.isEmpty()) {
            return "";
        }
        StringBuilder grace = new StringBuilder("{");
        ornaments.stream()
                .sorted(Comparator.comparingInt(note -> Note.parse(note.name()).midi()))
                .forEach(note -> grace.append(pitch(note)));
        return grace.append('}').toString();
    }

    private static String chord(List<NoteEvent> starting) {
        if (starting.size() == 1) {
            return pitch(starting.get(0));
        }
        StringBuilder chord = new StringBuilder("[");
        starting.stream()
                .sorted(Comparator.comparingInt(note -> Note.parse(note.name()).midi()))
                .forEach(note -> chord.append(pitch(note)));
        return chord.append(']').toString();
    }

    /** Reuses the engine's own ABC spelling, so an F sharp is written as one. */
    private static String pitch(NoteEvent note) {
        return fr.lapetina.music.theory.AbcNotation.pitch(Note.parse(note.name()));
    }

    /**
     * A length in eighth-note units.
     *
     * <p>ABC reads {@code n/d} as the unit multiplied by n over d, so an eighth is written
     * as nothing at all, a quarter as {@code 2}, a sixteenth as {@code /2} and a dotted
     * eighth as {@code 3/2}. The denominators tried cover dyadic rhythms and triplets, which
     * is what these corpora contain.
     */
    static String length(double duration) {
        return lengthOfUnits(duration / UNIT);
    }

    /** The same, for a length already expressed in eighth-note units. */
    static String lengthOfUnits(double units) {
        if (units <= 0) {
            return "";
        }
        for (int denominator : DENOMINATORS) {
            double scaled = units * denominator;
            long numerator = Math.round(scaled);
            if (numerator >= 1 && Math.abs(scaled - numerator) < 1e-6) {
                return render(numerator, denominator);
            }
        }
        // Nothing fitted, so round to the nearest eighth rather than write a wrong rhythm
        // as though it were exact.
        return render(Math.max(1, Math.round(units)), 1);
    }

    /**
     * Denominators tried when writing a length, dyadic first.
     *
     * <p>The odd ones matter: this repertoire contains quintuplets and septuplets, and a
     * length that does not fit any of these is rounded, which makes the bar the wrong
     * length. Better to write an unusual fraction than a wrong duration.
     */
    private static final int[] DENOMINATORS = {1, 2, 4, 8, 16, 32, 3, 6, 12, 24, 5, 10, 20, 40, 7, 14, 9};

    private static String render(long numerator, int denominator) {
        if (denominator == 1) {
            return numerator == 1 ? "" : Long.toString(numerator);
        }
        return numerator == 1 ? "/" + denominator : numerator + "/" + denominator;
    }
}
