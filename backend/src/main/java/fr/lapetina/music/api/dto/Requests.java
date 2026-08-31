package fr.lapetina.music.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request bodies, kept together because each is a couple of fields. */
public final class Requests {

    private Requests() {
    }

    /** Free text reaches the model's prompt, so it is bounded. */
    public record MessageRequest(@NotBlank @Size(max = 2000) String message) {
    }

    public record TextAnswerRequest(@Size(max = 2000) String answer) {
    }

    /** Ten octaves of simultaneous notes is already generous for two hands. */
    public record MidiAnswerRequest(@NotEmpty @Size(max = 128) List<Integer> notes) {
    }

    public record ChordAnalysisRequest(List<Integer> midiNotes, String notes, String key) {
    }

    public record ProgressionRequest(@NotEmpty List<String> chords, @NotBlank String key) {
    }

    public record NotationRequest(@NotBlank String kind, @NotBlank String content, String key) {
    }

    public record ExerciseRequest(@NotBlank String conceptId, Double difficulty, String answerMode) {
    }
}
