package fr.lapetina.music.knowledge.router;

/**
 * Something the application worked out for itself.
 *
 * <p>{@code operation} records what was computed, so a response can later say how it knew.
 * {@code answer} is the bare result — "D F# A C" — kept separate from the sentence so the
 * two cannot drift apart when the sentence is rewritten.
 */
public record TheoryAnswer(Kind kind, String operation, String statement, String answer, Object detail) {

    public enum Kind {
        ROMAN_NUMERAL,
        CHORD,
        SCALE,
        INTERVAL,
        PROGRESSION
    }
}
