package fr.lapetina.music.knowledge.harmony;

/**
 * Where a musical example came from.
 *
 * <p>Every example the tutor shows carries one of these, and the distinction is never
 * blurred. A generated example is a perfectly good teaching device; presenting one as
 * though Beethoven wrote it is a lie a learner has no way to catch.
 */
public enum ExampleOrigin {

    /** Taken from an annotated corpus. Carries work, movement, measure and attribution. */
    VERIFIED_CORPUS,

    /** Built by the theory engine for teaching. Never inherits a source's attribution. */
    GENERATED,

    /** Supplied by the learner. */
    USER_PROVIDED
}
