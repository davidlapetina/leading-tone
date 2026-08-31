package fr.lapetina.music.exercise;

/**
 * One way a concept can be practised: a kind of task, on a particular channel.
 *
 * <p>A concept's set of shapes is its menu. The tutor rotates through it so the same
 * question is not asked twice running, and a learner who has stated a preference is served
 * from the part of it they asked for.
 */
public record ExerciseShape(TaskKind kind, AnswerMode mode) {

    public static ExerciseShape write(TaskKind kind) {
        return new ExerciseShape(kind, AnswerMode.TEXT);
    }

    public static ExerciseShape play(TaskKind kind) {
        return new ExerciseShape(kind, AnswerMode.MIDI);
    }

    public boolean isPlayed() {
        return mode == AnswerMode.MIDI;
    }

    @Override
    public String toString() {
        return kind + "/" + mode;
    }
}
