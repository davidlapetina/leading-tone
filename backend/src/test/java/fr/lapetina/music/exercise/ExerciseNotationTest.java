package fr.lapetina.music.exercise;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Notation shown with a question must not answer it.
 *
 * <p>"Which minor scale is this: B C# D E F# G A#?" was drawn under a staff titled
 * "B harmonic minor", and "In C major, which chord is V?" drew the chord with "G" written
 * above it. Both marked correct, both taught nothing, and both fed the learner model
 * evidence of mastery that was never demonstrated. Every test passed throughout.
 */
class ExerciseNotationTest {

    private static final List<Double> DIFFICULTIES = List.of(0.2, 0.5, 0.85);
    private static final int ROUNDS = 8;

    @Test
    @DisplayName("no exercise draws notation that spells out its own answer")
    void notationNeverGivesTheAnswerAway() {
        ConceptGraph graph = new ConceptGraph(new ObjectMapper());
        ExerciseGenerator generator = new ExerciseGenerator(new Random(20260901L));
        List<String> leaks = new ArrayList<>();
        int withNotation = 0;

        for (Concept concept : graph.all()) {
            for (double difficulty : DIFFICULTIES) {
                for (ExerciseShape shape : ExerciseGenerator.shapesFor(concept.id())) {
                    for (int round = 0; round < ROUNDS; round++) {
                        ExerciseSpec spec = generator.generate(concept.id(), difficulty, shape);
                        String abc = spec.notationAbc();
                        if (abc == null || abc.isBlank()) {
                            continue;
                        }
                        withNotation++;
                        if (abc.contains("T:")) {
                            leaks.add("%s: notation is titled, which names what it draws: %s"
                                    .formatted(concept.id(), abc.lines().filter(l -> l.startsWith("T:")).toList()));
                        }
                        String answer = spec.expectedAnswer().canonical();
                        if (answer == null || answer.isBlank() || spec.prompt().contains(answer)) {
                            continue;   // the prompt says it too: that is the question, not a leak
                        }
                        for (String label : labelsAbove(abc)) {
                            if (label.equalsIgnoreCase(answer)) {
                                leaks.add("%s: \"%s\" answered \"%s\", written above the staff"
                                        .formatted(concept.id(), spec.prompt(), label));
                            }
                        }
                    }
                }
            }
        }
        assertTrue(withNotation > 40, "the sweep should have found notation to check, found " + withNotation);
        assertTrue(leaks.isEmpty(), "Notation gave the answer away:\n" + String.join("\n", leaks));
    }

    /** The text written above the staff: chord symbols and annotations, which a reader reads. */
    private static List<String> labelsAbove(String abc) {
        List<String> labels = new ArrayList<>();
        for (String line : abc.lines().toList()) {
            if (line.length() > 1 && line.charAt(1) == ':') {
                continue;
            }
            int from = line.indexOf('"');
            while (from >= 0) {
                int to = line.indexOf('"', from + 1);
                if (to < 0) {
                    break;
                }
                labels.add(line.substring(from + 1, to).replace("^", "").trim().toLowerCase(Locale.ROOT));
                from = line.indexOf('"', to + 1);
            }
        }
        return labels;
    }
}
