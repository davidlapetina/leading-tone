package fr.lapetina.music.exercise;

import java.util.List;

/**
 * What counts as a right answer, stored with the exercise so an attempt can be judged
 * long after the exercise was generated.
 *
 * @param canonical    the answer as the tutor would write it
 * @param acceptable   other accepted spellings and phrasings
 * @param chordSymbol  for MIDI chord answers, e.g. {@code G/B}
 * @param noteNames    for note-set answers, e.g. {@code [D, F#, A]}
 */
public record ExpectedAnswer(
        ExpectedAnswerKind kind,
        String canonical,
        List<String> acceptable,
        String chordSymbol,
        String scaleTonic,
        String scaleType,
        List<String> noteNames) {

    public static ExpectedAnswer text(String canonical, String... acceptable) {
        return new ExpectedAnswer(ExpectedAnswerKind.TEXT, canonical, List.of(acceptable), null, null, null, null);
    }

    public static ExpectedAnswer noteSet(List<String> notes) {
        return new ExpectedAnswer(ExpectedAnswerKind.NOTE_SET, String.join(" ", notes), List.of(),
                null, null, null, List.copyOf(notes));
    }

    public static ExpectedAnswer noteSequence(List<String> notes) {
        return new ExpectedAnswer(ExpectedAnswerKind.NOTE_SEQUENCE, String.join(" ", notes), List.of(),
                null, null, null, List.copyOf(notes));
    }

    public static ExpectedAnswer midiChord(String chordSymbol, String description) {
        return new ExpectedAnswer(ExpectedAnswerKind.MIDI_CHORD, description, List.of(),
                chordSymbol, null, null, null);
    }

    public static ExpectedAnswer midiScale(String tonic, String scaleType, String description) {
        return new ExpectedAnswer(ExpectedAnswerKind.MIDI_SCALE, description, List.of(),
                null, tonic, scaleType, null);
    }

    public static ExpectedAnswer midiNotes(List<String> notes, String description) {
        return new ExpectedAnswer(ExpectedAnswerKind.MIDI_NOTES, description, List.of(),
                null, null, null, List.copyOf(notes));
    }

    public static ExpectedAnswer explanation(String canonical) {
        return new ExpectedAnswer(ExpectedAnswerKind.EXPLANATION, canonical, List.of(), null, null, null, null);
    }
}
