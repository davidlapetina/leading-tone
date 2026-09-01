package fr.lapetina.music.theory;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders theory objects as ABC notation, which the frontend hands straight to abcjs for
 * an SVG score and for playback.
 */
public final class AbcNotation {

    /**
     * The octave a chord is written in when it is shown on its own.
     *
     * <p>Chords are engraved around middle C, which puts a root-position seventh chord on
     * and just below the treble staff where it can be read at a glance. An octave lower
     * pushes every note onto ledger lines, which is legible only in the sense that a
     * musician can work it out.
     */
    public static final int CHORD_OCTAVE = 4;

    private AbcNotation() {
    }

    /** ABC pitch: C4 is {@code C}, C5 is {@code c}, C3 is {@code C,}, C6 is {@code c'}. */
    public static String pitch(Note note) {
        StringBuilder builder = new StringBuilder();
        builder.append(accidental(note.pitchClass().accidental()));
        String letter = note.pitchClass().letter().name();
        int octave = note.octave();
        if (octave >= 5) {
            builder.append(letter.toLowerCase());
            builder.append("'".repeat(octave - 5));
        } else {
            builder.append(letter);
            builder.append(",".repeat(Math.max(0, 4 - octave)));
        }
        return builder.toString();
    }

    private static String accidental(Accidental accidental) {
        return switch (accidental) {
            case DOUBLE_FLAT -> "__";
            case FLAT -> "_";
            case NATURAL -> "";
            case SHARP -> "^";
            case DOUBLE_SHARP -> "^^";
        };
    }

    public static String keyField(Key key) {
        return key.tonic().name() + (key.mode() == Mode.MINOR ? "m" : "");
    }

    private static String header(String title, Key key, String noteLength) {
        return header(title, key, noteLength, "4/4");
    }

    /**
     * @param metre {@code none} for something that has no metre, such as a scale. A scale of
     *     six notes barred every four leaves a bar three beats long, which is not a bar of
     *     anything; without a metre there is no bar to be short.
     */
    private static String header(String title, Key key, String noteLength, String metre) {
        // No T: line. Wherever notation is shown it already has a label -- a lesson caption, an
        // exercise prompt, a citation -- and a title that names what it draws is worse than
        // redundant when the exercise is "which scale is this": it prints the answer above the
        // staff. The title is kept as a parameter because it reads as the caller's intent.
        return """
                X:1
                M:%s
                L:%s
                K:%s
                """.formatted(metre, noteLength, keyField(key));
    }

    /** A single chord as an ABC chord group, labelled with its lead-sheet symbol. */
    public static String chord(Chord chord, int bassOctave, Key key) {
        String body = chordGroup(chord, bassOctave);
        return header(chord.describe(), key, "1/1") + "\"" + chord.symbol() + "\"" + body + "|]\n";
    }

    private static String chordGroup(Chord chord, int bassOctave) {
        StringBuilder builder = new StringBuilder("[");
        for (Note note : chord.notes(bassOctave)) {
            builder.append(pitch(note));
        }
        return builder.append(']').toString();
    }

    /** An ascending scale, one note per beat, barred every four notes. */
    public static String scale(Scale scale, int octave) {
        // Only a seven-note scale has a key signature. A whole-tone or blues scale written
        // under "K:Cm" would be engraved with flats it does not contain, so those are
        // written in C with every accidental spelled out instead.
        Key key = scale.type().isHeptatonic()
                ? new Key(scale.tonic(), scale.type() == ScaleType.MAJOR ? Mode.MAJOR : Mode.MINOR)
                : Key.major("C");
        StringBuilder body = new StringBuilder();
        List<Note> notes = scale.notes(octave);
        for (int i = 0; i < notes.size(); i++) {
            if (i > 0 && i % 4 == 0) {
                body.append('|');
            }
            body.append(pitch(notes.get(i)));
        }
        return header(scale.name(), key, "1/4", "none") + body + "|]\n";
    }

    /** A chord progression, one chord per bar, labelled with symbols above the staff. */
    public static String progression(List<Chord> chords, Key key, int bassOctave) {
        StringBuilder body = new StringBuilder();
        for (Chord chord : chords) {
            body.append('"').append(chord.symbol()).append('"')
                    .append(chordGroup(chord, bassOctave)).append('|');
        }
        if (body.length() > 0) {
            body.setLength(body.length() - 1);
        }
        return header("Progression in " + key.name(), key, "1/1") + body + "|]\n";
    }

    /** A bare sequence of notes, for melodic and interval work. */
    public static String melody(List<Note> notes, Key key, String title) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) {
            if (i > 0 && i % 4 == 0) {
                body.append('|');
            }
            body.append(pitch(notes.get(i)));
        }
        return header(title, key, "1/4") + body + "|]\n";
    }

    /** Two notes sounding together, the usual way to show an interval. */
    public static String interval(Note lower, Interval interval, Key key) {
        Note upper = new Note(lower.pitchClass().transpose(interval),
                lower.octave() + (lower.pitchClass().semitone() + interval.semitones()) / 12);
        List<Note> notes = new ArrayList<>(List.of(lower, upper));
        return header(interval.symbol() + " above " + lower.name(), key, "1/1")
                + "[" + pitch(notes.get(0)) + pitch(notes.get(1)) + "]|]\n";
    }
}
