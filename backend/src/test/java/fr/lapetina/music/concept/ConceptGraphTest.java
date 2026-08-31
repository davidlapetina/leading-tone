package fr.lapetina.music.concept;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConceptGraphTest {

    private final ConceptGraph graph = new ConceptGraph(new ObjectMapper());

    @Test
    void loadsEveryConceptWithResolvablePrerequisites() {
        assertTrue(graph.size() >= 20);
        for (Concept concept : graph.all()) {
            for (String prerequisite : concept.prerequisites()) {
                assertTrue(graph.contains(prerequisite),
                        concept.id() + " requires missing " + prerequisite);
            }
        }
    }

    @Test
    @DisplayName("prerequisites always come before their dependents")
    void ordersTopologically() {
        List<String> order = graph.topologicalOrder();
        for (Concept concept : graph.all()) {
            int position = order.indexOf(concept.id());
            for (String prerequisite : concept.prerequisites()) {
                assertTrue(order.indexOf(prerequisite) < position,
                        prerequisite + " must precede " + concept.id());
            }
        }
    }

    @Test
    void walksPrerequisitesTransitively() {
        Set<String> all = Set.copyOf(graph.allPrerequisitesOf("secondary-dominant").stream()
                .map(Concept::id).toList());
        assertTrue(all.containsAll(Set.of("dominant-seventh", "seventh-chord", "triad", "interval", "note")));
        assertFalse(all.contains("modulation"));
    }

    @Test
    @DisplayName("the frontier is what can be taught next, and nothing else")
    void computesTheTeachableFrontier() {
        List<String> frontier = graph.frontier(id -> false).stream().map(Concept::id).toList();
        assertEquals(List.of("note"), frontier);

        Set<String> known = Set.of("note", "interval", "major-scale", "triad");
        List<String> next = graph.frontier(known::contains).stream().map(Concept::id).toList();
        assertTrue(next.contains("diatonic-triads"));
        assertTrue(next.contains("chord-inversion"));
        assertTrue(next.contains("seventh-chord"));
        assertFalse(next.contains("modulation"));
        assertFalse(next.contains("note"));
    }

    @Test
    void reportsWhatIsMissingBeforeAConcept() {
        List<String> missing = graph.missingPrerequisites("modulation", id -> id.equals("note"))
                .stream().map(Concept::id).toList();
        assertTrue(missing.contains("secondary-dominant"));
        assertFalse(missing.contains("note"));
    }

    @Test
    void knowsWhatEachConceptUnlocks() {
        List<String> unlocked = graph.dependentsOf("triad").stream().map(Concept::id).toList();
        assertTrue(unlocked.contains("chord-inversion"));
        assertTrue(unlocked.contains("seventh-chord"));
    }

    @Test
    void rejectsUnknownConcepts() {
        assertThrows(IllegalArgumentException.class, () -> graph.require("harmony-in-general"));
    }
}
