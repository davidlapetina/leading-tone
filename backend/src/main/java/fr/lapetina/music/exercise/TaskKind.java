package fr.lapetina.music.exercise;

/**
 * What the learner is being asked to <em>do</em>, as distinct from the channel they answer
 * on.
 *
 * <p>Asking someone to name a chord, to build one, and to say what it is doing in a key
 * are three different skills that happen to share a concept. Practising only one of them
 * is what made the tutor repeat itself, and it is why mastery could be earned without ever
 * using the concept in context.
 */
public enum TaskKind {

    /** Given the material, name it: these notes, which chord? */
    IDENTIFY,

    /** Given the name, produce it: spell it, or play it. */
    BUILD,

    /** Given a musical context, say what the thing is doing in it. */
    ANALYSE,

    /** Use it somewhere it has not been seen: a new key, a longer progression. */
    APPLY;

    public String describe() {
        return switch (this) {
            case IDENTIFY -> "name it";
            case BUILD -> "build it";
            case ANALYSE -> "explain it in context";
            case APPLY -> "use it somewhere new";
        };
    }
}
