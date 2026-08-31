package fr.lapetina.music.exercise;

/** How an answer is checked. Everything but {@link #EXPLANATION} is decided deterministically. */
public enum ExpectedAnswerKind {

    TEXT,
    NOTE_SET,
    NOTE_SEQUENCE,
    MIDI_CHORD,
    MIDI_SCALE,
    MIDI_NOTES,

    /**
     * A free-form explanation. The language model may propose a verdict, but it arrives
     * as a low-confidence proposal and is weighted accordingly.
     */
    EXPLANATION
}
