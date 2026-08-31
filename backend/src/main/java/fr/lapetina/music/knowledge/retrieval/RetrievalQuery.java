package fr.lapetina.music.knowledge.retrieval;

import fr.lapetina.music.knowledge.index.MusicSymbols;
import java.util.List;

/**
 * What to look for.
 *
 * @param conceptId the concept currently being taught, used as a metadata boost rather
 *     than a filter, so a good passage filed under a neighbouring concept is not excluded
 */
public record RetrievalQuery(String text, String conceptId, int limit) {

    public static RetrievalQuery of(String text) {
        return new RetrievalQuery(text, null, 4);
    }

    /** The harmonic symbols in the query, matched exactly rather than as words. */
    public List<String> symbols() {
        return MusicSymbols.extract(text);
    }

    public boolean isBlank() {
        return text == null || text.isBlank();
    }
}
