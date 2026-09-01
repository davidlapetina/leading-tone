package fr.lapetina.music.exercise;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import fr.lapetina.music.learner.EvidenceResult;
import fr.lapetina.music.midi.MidiEvaluator;
import fr.lapetina.music.midi.MidiPerformance;
import fr.lapetina.music.theory.Note;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A played answer is accepted when it is the right notes, and only then.
 *
 * <p>The typed path had the opposite problem -- an answer counted wherever the expected words
 * appeared inside it, so a different scale entirely was marked correct. This checks the
 * played path does not have its own version of that, by playing every other exercise's answer
 * at every exercise and requiring all of them to be refused.
 */
class MidiMarkingTest {

    @Test
    @DisplayName("a played answer is accepted only when it is the notes that were asked for")
    void acceptsTheRightNotesAndNothingElse() {
        ConceptGraph graph = new ConceptGraph(new ObjectMapper());
        ExerciseEvaluator evaluator = new ExerciseEvaluator();
        evaluator.midiEvaluator = new MidiEvaluator();
        ExerciseGenerator generator = new ExerciseGenerator(new Random(11L));

        List<ExerciseSpec> played = new ArrayList<>();
        for (Concept concept : graph.all()) {
            for (double difficulty : List.of(0.2, 0.5, 0.85)) {
                for (ExerciseShape shape : ExerciseGenerator.shapesFor(concept.id())) {
                    if (!shape.isPlayed()) {
                        continue;
                    }
                    for (int round = 0; round < 6; round++) {
                        played.add(generator.generate(concept.id(), difficulty, shape));
                    }
                }
            }
        }

        int rightAccepted = 0, wrongAccepted = 0, tried = 0;
        List<String> leaks = new ArrayList<>();
        for (ExerciseSpec spec : played) {
            List<Integer> correct = notesFor(spec.expectedAnswer());
            if (correct.isEmpty()) {
                continue;
            }
            if (evaluator.evaluateMidi(spec.expectedAnswer(), performance(correct), spec.keyContext())
                    .result() == EvidenceResult.CORRECT) {
                rightAccepted++;
            }
            for (ExerciseSpec other : played) {
                List<Integer> wrong = notesFor(other.expectedAnswer());
                if (wrong.isEmpty() || wrong.equals(correct)
                        || other.expectedAnswer().kind() != spec.expectedAnswer().kind()) {
                    continue;
                }
                tried++;
                if (evaluator.evaluateMidi(spec.expectedAnswer(), performance(wrong), spec.keyContext())
                        .result() == EvidenceResult.CORRECT) {
                    wrongAccepted++;
                    if (leaks.size() < 40) {
                        leaks.add("%s | \"%s\" wants %s but accepted %s"
                                .formatted(spec.type(), spec.prompt(), correct, wrong));
                    }
                }
                if (tried > 20000) {
                    break;
                }
            }
        }
        assertTrue(tried > 5000, "the sweep should have had plenty to try, had " + tried);
        assertEquals(played.size(), rightAccepted,
                "every played exercise must accept the notes it asked for");
        assertTrue(leaks.isEmpty(), "Played answers accepted the wrong notes:\n"
                + String.join("\n", leaks.stream().distinct().limit(10).toList()));
    }

    private static List<Integer> notesFor(ExpectedAnswer expected) {
        return switch (expected.kind()) {
            case MIDI_CHORD -> fr.lapetina.music.theory.ChordAnalyzer.parse(expected.chordSymbol())
                    .notes(4).stream().map(Note::midi).toList();
            case MIDI_SCALE -> new Scale(PitchClass.parse(expected.scaleTonic()),
                    ScaleType.valueOf(expected.scaleType())).notes(4).stream().map(Note::midi).toList();
            case MIDI_NOTES, NOTE_SET, NOTE_SEQUENCE -> expected.noteNames() == null ? List.of()
                    : expected.noteNames().stream().map(n -> new Note(PitchClass.parse(n), 4).midi()).toList();
            default -> List.of();
        };
    }

    private static MidiPerformance performance(List<Integer> notes) {
        return MidiPerformance.of(notes);
    }
}
