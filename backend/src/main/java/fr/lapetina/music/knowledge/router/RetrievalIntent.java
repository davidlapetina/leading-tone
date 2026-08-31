package fr.lapetina.music.knowledge.router;

/**
 * What a learner is actually asking for.
 *
 * <p>The distinction matters because the three kinds of answer come from different places
 * and have different failure modes. A calculation that is guessed is wrong; an explanation
 * that is calculated is dry; an example that is invented is a fabrication with a composer's
 * name on it. Routing them to the same place is how all three happen.
 *
 * <p>One question can carry several: "explain V/V and give me a Beethoven example" is a
 * calculation, an explanation and a corpus lookup.
 */
public enum RetrievalIntent {

    /** "Why does the leading tone resolve upward?" — answered from published prose. */
    CONCEPT_EXPLANATION,

    /** "Give me a Beethoven example of V/V." — answered from annotated scores, or not at all. */
    HARMONIC_EXAMPLE,

    /** "What is V7/V in C major?" — answered by arithmetic, never by a language model. */
    DETERMINISTIC_CALCULATION,

    /** "Test me on secondary dominants." */
    EXERCISE_REQUEST,

    /** An answer to an open question, rather than a new question. */
    ANSWER_CHECK,

    /** "What do I still not know?" */
    STUDENT_DIAGNOSTIC
}
