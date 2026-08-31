package fr.lapetina.music.concept;

import java.util.List;

/**
 * A concept, explained. The reading half of the application: what this is, why it matters,
 * and worked examples — before anything is asked.
 */
public record Lesson(
        String conceptId,
        String name,
        String summary,
        String category,
        List<LessonSection> sections,
        List<ConceptLink> restsOn,
        List<ConceptLink> opensUp,
        double mastery,
        String state,
        boolean ready) {

    /** A neighbouring concept, with enough about it to render a link. */
    public record ConceptLink(String conceptId, String name, double mastery, boolean known) {
    }
}
