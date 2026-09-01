package fr.lapetina.music.knowledge.harmony;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The mapping from a concept to the harmonies that illustrate it.
 *
 * <p>Every way this can be wrong is silent. A concept id that does not exist, a concept
 * declared to be a progression with no progression to find, numerals listed for a concept
 * that never reads them — none of these fail, they just produce a lesson with an empty
 * examples section, which reads as "there are no examples of this in real music" and is a
 * different claim from the truth.
 *
 * <p>What cannot be checked here is whether a corpus actually uses a label: that needs the
 * corpora, and these tests run without them. The labels differ between corpora — the
 * treebank writes {@code bII} where the scores write {@code bII7} — so a new entry should be
 * checked against a running instance as well as against this.
 */
class ConceptExampleMappingTest {

    private final ConceptGraph graph = new ConceptGraph(new ObjectMapper());

    @Test
    @DisplayName("every concept named in the mapping is a concept that exists")
    void namesOnlyRealConcepts() {
        Set<String> real = graph.all().stream().map(Concept::id).collect(Collectors.toSet());
        List<String> unknown = new ArrayList<>();
        for (String id : ConceptExamples.NUMERALS.keySet()) {
            if (!real.contains(id)) {
                unknown.add("NUMERALS: " + id);
            }
        }
        for (String id : ConceptExamples.PROGRESSIONS.keySet()) {
            if (!real.contains(id)) {
                unknown.add("PROGRESSIONS: " + id);
            }
        }
        for (String id : ConceptExamples.CADENCES.keySet()) {
            if (!real.contains(id)) {
                unknown.add("CADENCES: " + id);
            }
        }
        for (String id : ConceptExamples.IS_A_PROGRESSION) {
            if (!real.contains(id)) {
                unknown.add("IS_A_PROGRESSION: " + id);
            }
        }
        assertTrue(unknown.isEmpty(), "Mapped to concepts that do not exist: " + unknown);
    }

    @Test
    @DisplayName("a concept declared to be a progression has a progression to find")
    void everyProgressionConceptHasAPattern() {
        List<String> without = ConceptExamples.IS_A_PROGRESSION.stream()
                .filter(id -> ConceptExamples.PROGRESSIONS.getOrDefault(id, List.of()).isEmpty())
                .toList();
        assertTrue(without.isEmpty(),
                "These can never return anything: they refuse a single chord and have no pattern: " + without);
    }

    @Test
    @DisplayName("numerals are not listed for a concept that will never read them")
    void noDeadNumerals() {
        // A concept that is a progression ignores its numerals, so listing them reads as
        // configuration that does something and is not.
        List<String> dead = ConceptExamples.IS_A_PROGRESSION.stream()
                .filter(ConceptExamples.NUMERALS::containsKey)
                .toList();
        assertTrue(dead.isEmpty(), "Numerals listed but never read: " + dead);
    }

    @Test
    @DisplayName("a pattern is at least two chords, or it is not a progression")
    void patternsAreProgressions() {
        List<String> tooShort = new ArrayList<>();
        ConceptExamples.PROGRESSIONS.forEach((id, patterns) -> patterns.stream()
                .filter(pattern -> pattern.size() < 2)
                .forEach(pattern -> tooShort.add(id + " " + pattern)));
        assertTrue(tooShort.isEmpty(), "The search ignores these silently: " + tooShort);
    }
}
