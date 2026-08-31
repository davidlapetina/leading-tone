package fr.lapetina.music.concept;

import java.util.List;

/**
 * A unit of musical knowledge. Concepts carry prerequisites, which is a statement about
 * what depends on what — not about the order anything gets taught in.
 *
 * @param intrinsicDifficulty 0..1, used to scale how much evidence an answer is worth
 */
public record Concept(
        String id,
        String name,
        String description,
        List<String> prerequisites,
        ConceptCategory category,
        double intrinsicDifficulty) {
}
