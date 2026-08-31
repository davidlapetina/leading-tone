package fr.lapetina.music.tutor;

import fr.lapetina.music.exercise.AnswerMode;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * Notices when the learner says how they would rather practise.
 *
 * <p>The tutor decides <em>what</em> to work on from the learner model, and that does not
 * change. This is about the other axis — whether to play it or write it — which is a
 * matter of preference rather than pedagogy, and which the learner knows better than the
 * inference does.
 */
@ApplicationScoped
public class PracticeChoiceDetector {

    private static final List<String> WANTS_KEYBOARD = List.of(
            "let me play", "i want to play", "id rather play", "i would rather play",
            "ask me to play", "on the keyboard", "at the keyboard", "play them",
            "play these", "playing instead", "more playing", "keyboard exercises");

    private static final List<String> WANTS_WRITING = List.of(
            "let me write", "i want to write", "id rather write", "i would rather write",
            "ask me to spell", "ask me to name", "type instead", "writing instead",
            "not the keyboard", "no keyboard", "without the keyboard", "written exercises");

    private static final List<String> WANTS_EITHER = List.of(
            "you choose", "you decide", "either is fine", "whatever you think",
            "mix them up", "vary it", "up to you");

    /**
     * The choice expressed, if any. An empty result means nothing was asked for; a result
     * holding an empty {@code AnswerMode} means the learner handed the choice back.
     */
    public Optional<Optional<AnswerMode>> detect(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        String text = " " + message.toLowerCase().replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ") + " ";

        if (contains(text, WANTS_EITHER)) {
            return Optional.of(Optional.empty());
        }
        if (contains(text, WANTS_WRITING)) {
            return Optional.of(Optional.of(AnswerMode.TEXT));
        }
        if (contains(text, WANTS_KEYBOARD)) {
            return Optional.of(Optional.of(AnswerMode.MIDI));
        }
        return Optional.empty();
    }

    private static boolean contains(String text, List<String> phrases) {
        return phrases.stream().anyMatch(phrase -> text.contains(" " + phrase + " "));
    }
}
