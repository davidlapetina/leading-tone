package fr.lapetina.music.knowledge.harmony;

/**
 * One note in a score, as the corpus publishes it.
 *
 * <p>Positions and durations are fractions of a whole note, exactly as given, so nothing is
 * lost to floating point before it reaches the engraver.
 *
 * @param name the spelled pitch with its octave, e.g. {@code F#4} — spelling, not a MIDI number
 */
public record NoteEvent(
        int measure,
        double onset,
        double duration,
        int staff,
        int voice,
        String name,
        boolean tied,
        /** The metre in force at this note, as published. Corpora do change metre mid-piece. */
        String timeSignature) {

    public NoteEvent(int measure, double onset, double duration, int staff, int voice,
                     String name, boolean tied) {
        this(measure, onset, duration, staff, voice, name, tied, null);
    }
}
