package fr.lapetina.music.midi;

import fr.lapetina.music.learner.EvidenceResult;
import java.util.List;

/**
 * The verdict on something played, detailed enough for the tutor to say what went wrong
 * rather than just "no".
 *
 * @param misconceptionCode a stable code when the mistake is a recognisable one, else null
 */
public record MidiEvaluation(
        EvidenceResult result,
        boolean correctPitchClasses,
        boolean correctBass,
        List<String> missing,
        List<String> extra,
        String expected,
        String detected,
        String feedback,
        String misconceptionCode,
        String misconceptionDescription) {

    public boolean isCorrect() {
        return result == EvidenceResult.CORRECT;
    }

    public boolean hasMisconception() {
        return misconceptionCode != null;
    }
}
