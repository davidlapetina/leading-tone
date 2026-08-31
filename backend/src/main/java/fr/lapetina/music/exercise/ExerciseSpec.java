package fr.lapetina.music.exercise;

import fr.lapetina.music.learner.EvidenceType;
import java.util.List;

/** A generated exercise, before it is persisted and shown. */
public record ExerciseSpec(
        String conceptId,
        ExerciseType type,
        TaskKind taskKind,
        AnswerMode answerMode,
        EvidenceType evidenceType,
        String prompt,
        ExpectedAnswer expectedAnswer,
        String notationAbc,
        String keyContext,
        double difficulty,
        Scaffold scaffold,
        List<String> choices) {

    public ExerciseSpec(String conceptId, ExerciseType type, TaskKind taskKind, AnswerMode answerMode,
                        EvidenceType evidenceType, String prompt, ExpectedAnswer expectedAnswer,
                        String notationAbc, String keyContext, double difficulty) {
        this(conceptId, type, taskKind, answerMode, evidenceType, prompt, expectedAnswer, notationAbc,
                keyContext, difficulty, Scaffold.NONE, List.of());
    }
}
