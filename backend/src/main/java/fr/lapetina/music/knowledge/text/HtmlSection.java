package fr.lapetina.music.knowledge.text;

/**
 * One heading-delimited part of a document.
 *
 * @param heading the section's own heading, or empty for the text before the first one
 * @param order position within the document, used for stable ordering and citation
 */
public record HtmlSection(String heading, int level, String text, int order) {

    public boolean isIntroduction() {
        return heading == null || heading.isBlank();
    }
}
