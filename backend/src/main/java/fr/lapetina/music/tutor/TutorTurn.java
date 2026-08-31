package fr.lapetina.music.tutor;

import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.exercise.AttemptResult;
import java.util.UUID;

/**
 * One completed turn of teaching: what was said, why, and what the learner is now
 * expected to do.
 *
 * @param attempt the evaluation of the previous answer, when this turn followed one
 */
public record TutorTurn(
        UUID sessionId,
        UUID interactionId,
        String message,
        TeachingAction action,
        String conceptId,
        String conceptName,
        String rationale,
        double difficulty,
        boolean expectsAnswer,
        AnswerMode answerMode,
        UUID exerciseId,
        String exercisePrompt,
        /** What the learner is being asked to do: recognise, produce, or explain in context. */
        String taskKind,
        String notationAbc,
        AttemptResult attempt,
        String narrator) {
}
