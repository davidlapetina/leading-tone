package fr.lapetina.music.learner;

/**
 * A readable label derived from mastery, confidence and review timing. Nothing writes
 * this directly — it is always recomputed from the numbers.
 */
public enum LearningState {

    UNKNOWN,
    INTRODUCED,
    LEARNING,
    PRACTICING,
    RELIABLE,
    MASTERED,
    NEEDS_REVIEW;

    public boolean isAtLeastPracticing() {
        return this == PRACTICING || this == RELIABLE || this == MASTERED || this == NEEDS_REVIEW;
    }
}
