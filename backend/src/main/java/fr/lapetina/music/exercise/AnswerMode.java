package fr.lapetina.music.exercise;

/** How the learner is expected to answer. */
public enum AnswerMode {

    /** Free text, evaluated against a spelled expected answer. */
    TEXT,

    /** Notes played on the keyboard, evaluated deterministically from MIDI. */
    MIDI,

    /** One of a fixed set of options. */
    MULTIPLE_CHOICE,

    /** Nothing is expected back; the tutor is explaining. */
    NONE
}
