package fr.lapetina.music.tutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.exercise.AnswerMode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PracticeChoiceDetectorTest {

    private final PracticeChoiceDetector detector = new PracticeChoiceDetector();

    private AnswerMode choiceIn(String message) {
        return detector.detect(message).orElseThrow().orElse(null);
    }

    @Test
    void hearsARequestToPlay() {
        for (String text : new String[]{"let me play these", "I'd rather play them",
                "can we do more playing instead", "ask me to play"}) {
            assertEquals(AnswerMode.MIDI, choiceIn(text), text);
        }
    }

    @Test
    void hearsARequestToWrite() {
        for (String text : new String[]{"let me write these", "no keyboard for now",
                "ask me to spell them", "written exercises please"}) {
            assertEquals(AnswerMode.TEXT, choiceIn(text), text);
        }
    }

    @Test
    @DisplayName("handing the choice back clears it rather than picking one")
    void hearsTheChoiceBeingHandedBack() {
        assertEquals(Optional.of(Optional.empty()), detector.detect("you choose"));
        assertEquals(Optional.of(Optional.empty()), detector.detect("mix them up"));
    }

    @Test
    void staysOutOfTheWayOfOrdinaryAnswers() {
        for (String text : new String[]{"G7", "F Ab C", "a major third", "V7/V",
                "what is a secondary dominant", "I don't know", "Play"}) {
            assertTrue(detector.detect(text).isEmpty(), text);
        }
    }
}
