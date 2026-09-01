package fr.lapetina.music.concept;

import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.learner.LearnerService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A concept in the catalogue that opens to an empty page is a broken promise: it is listed
 * as something to learn, and then there is nothing to read.
 *
 * <p>Fifteen of thirty-six were in that state, which nothing caught, because the previous
 * test only asked that generating a lesson did not throw.
 */
@QuarkusTest
class EveryConceptHasALessonTest {

    @Inject
    ConceptGraph conceptGraph;

    @Inject
    LessonService lessonService;

    @Inject
    LearnerService learnerService;

    @Test
    @DisplayName("every concept offered in the catalogue has something written about it")
    void noConceptOpensToAnEmptyPage() {
        var snapshot = learnerService.snapshot(learnerService.current());
        List<String> empty = new ArrayList<>();

        for (Concept concept : conceptGraph.all()) {
            Lesson lesson = lessonService.lessonFor(concept.id(), snapshot);
            if (lesson.sections().isEmpty()) {
                empty.add(concept.id());
            }
        }
        assertTrue(empty.isEmpty(), "concepts with no lesson: " + String.join(", ", empty));
    }

    @Test
    @DisplayName("a lesson says something, rather than being a heading with no body")
    void everyLessonHasSubstance() {
        var snapshot = learnerService.snapshot(learnerService.current());
        List<String> thin = new ArrayList<>();

        for (Concept concept : conceptGraph.all()) {
            Lesson lesson = lessonService.lessonFor(concept.id(), snapshot);
            boolean everySectionSpeaks = lesson.sections().stream()
                    .allMatch(section -> section.points().stream()
                            .anyMatch(point -> point != null && point.length() > 40));
            if (!everySectionSpeaks) {
                thin.add(concept.id());
            }
        }
        assertTrue(thin.isEmpty(), "concepts with an empty-ish section: " + String.join(", ", thin));
    }
}
