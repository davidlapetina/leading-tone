package fr.lapetina.music.knowledge.text;

import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Marks a passage with the concepts it is about, so retrieval can prefer material on the
 * subject currently being taught.
 *
 * <p>A boost rather than a filter: a good explanation of secondary dominants filed under
 * tonicization is still a good explanation, and excluding it would be worse than ranking
 * it slightly lower.
 */
@ApplicationScoped
public class ConceptTagger {

    @Inject
    ConceptGraph conceptGraph;

    public String tag(String documentTitle, String sectionTitle, String body) {
        String haystack = (nullToEmpty(documentTitle) + " " + nullToEmpty(sectionTitle) + " " + nullToEmpty(body))
                .toLowerCase(Locale.ROOT);
        Set<String> matched = new LinkedHashSet<>();
        for (Concept concept : conceptGraph.all()) {
            for (String phrase : phrasesFor(concept)) {
                if (haystack.contains(phrase)) {
                    matched.add(concept.id());
                    break;
                }
            }
        }
        return String.join(",", matched);
    }

    /** The concept's name, plus the hyphen-free form of its id, which is often how it is written. */
    private static List<String> phrasesFor(Concept concept) {
        List<String> phrases = new ArrayList<>();
        phrases.add(concept.name().toLowerCase(Locale.ROOT));
        phrases.add(concept.id().replace('-', ' '));
        return phrases;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
