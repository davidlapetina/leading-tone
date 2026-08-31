package fr.lapetina.music.tutor;

/**
 * What the tutor should do next. The policy decides this from the learner model; the
 * language model carries it out but never chooses it.
 *
 * <p>Every action ends in something the learner has to do, including the two that begin
 * by explaining. A turn that only talks produces no evidence, and a concept that produces
 * no evidence can never move — which is how a tutor ends up asking the same question six
 * times in a row.
 */
public enum TeachingAction {

    /** Find out what is already known, without it feeling like a placement test. */
    DIAGNOSE,

    /** Present a concept for the first time, anchored to something already mastered. */
    INTRODUCE,

    /** Say more about a concept that has been met but has not settled. */
    EXPLAIN,

    /** Work the concept at roughly its current level. */
    PRACTICE,

    /** Shore up a prerequisite that is holding something else back. */
    REINFORCE,

    /** Push past comfortable ground. */
    CHALLENGE,

    /** Apply the concept somewhere it has not been seen before. */
    TRANSFER,

    /** Bring back something learned earlier that is due. */
    REVIEW,

    /** Address a specific wrong belief that has been observed more than once. */
    CORRECT_MISCONCEPTION,

    /** Answer something the learner asked about, rather than what was planned. */
    ANSWER_QUESTION;

    /**
     * The kind of task this action calls for. Challenging someone means asking them to
     * explain what a chord is doing, not to spell it again at a harder key; without this
     * the action changed only the difficulty and the wording.
     *
     * <p>Null means no preference, and the tutor rotates through the concept's forms.
     */
    public fr.lapetina.music.exercise.TaskKind preferredTaskKind() {
        return switch (this) {
            case DIAGNOSE, INTRODUCE, ANSWER_QUESTION -> fr.lapetina.music.exercise.TaskKind.IDENTIFY;
            case EXPLAIN, PRACTICE, REINFORCE, CORRECT_MISCONCEPTION -> fr.lapetina.music.exercise.TaskKind.BUILD;
            case CHALLENGE, TRANSFER -> fr.lapetina.music.exercise.TaskKind.ANALYSE;
            case REVIEW -> null;
        };
    }

    /**
     * True when the turn should say something before it asks something. The check for
     * understanding still follows — explaining and then testing is one turn, not two.
     */
    public boolean explainsBeforeAsking() {
        return this == INTRODUCE || this == EXPLAIN || this == CORRECT_MISCONCEPTION
                || this == ANSWER_QUESTION;
    }
}
