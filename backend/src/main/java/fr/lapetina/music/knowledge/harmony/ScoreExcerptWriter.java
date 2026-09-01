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
        abc.append("K:").append(keySignature == null || keySignature.isBlank() ? "C" : keySignature).append('\n');

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
        TreeSet<Double> onsets = new TreeSet<>();
        inBar.forEach(note -> onsets.add(note.onset()));

        // Built as events first, so runs of three can be recognised as tuplets afterwards.
        // Written straight out, a triplet eighth becomes a two-thirds length that engravers
        // draw as a dotted note -- a different rhythm from the one in the score.
        List<Event> events = new ArrayList<>();
        double cursor = 0.0;
        List<Double> ordered = new ArrayList<>(onsets);
        for (int i = 0; i < ordered.size(); i++) {
            double onset = ordered.get(i);
            if (onset > cursor + 1e-9) {
                events.add(new Event("z", (onset - cursor) / UNIT));
            }
            List<NoteEvent> starting = inBar.stream()
                    .filter(note -> Math.abs(note.onset() - onset) < 1e-9)
                    .toList();
            double shortest = starting.stream().mapToDouble(NoteEvent::duration).min().orElse(UNIT);
            double untilNext = i + 1 < ordered.size() ? ordered.get(i + 1) - onset : shortest;
            double written = Math.min(shortest, untilNext <= 0 ? shortest : untilNext);

            events.add(new Event(chord(starting), written / UNIT));
            cursor = onset + written;
        }
        return render(events);
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
    private static String render(List<Event> events) {
        StringBuilder bar = new StringBuilder();
        int i = 0;
        while (i < events.size()) {
            if (i + 2 < events.size() && isTripletRun(events, i)) {
                double notated = events.get(i).units() * 1.5;
                bar.append("(3");
                for (int n = i; n < i + 3; n++) {
                    bar.append(events.get(n).text()).append(lengthOfUnits(notated));
                }
                i += 3;
                continue;
            }
            bar.append(events.get(i).text()).append(lengthOfUnits(events.get(i).units()));
            i++;
        }
        return bar.toString();
    }

    /** Three equal events whose written length would need a denominator of three. */
    private static boolean isTripletRun(List<Event> events, int at) {
        double units = events.get(at).units();
        if (units <= 0 || Math.abs(units * 3 - Math.round(units * 3)) > 1e-6) {
            return false;
        }
        if (Math.abs(units * 2 - Math.round(units * 2)) < 1e-6) {
            return false;   // already a plain note length; nothing to group
        }
        for (int n = at + 1; n < at + 3; n++) {
            if (Math.abs(events.get(n).units() - units) > 1e-6) {
                return false;
            }
        }
        return true;
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

    private static final int[] DENOMINATORS = {1, 2, 4, 8, 16, 3, 6, 12};

    private static String render(long numerator, int denominator) {
        if (denominator == 1) {
            return numerator == 1 ? "" : Long.toString(numerator);
        }
        return numerator == 1 ? "/" + denominator : numerator + "/" + denominator;
    }
}
