package fr.lapetina.music.knowledge.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a chapter into its headed sections.
 *
 * <p>Sections are the natural teaching unit in a textbook, so they are the unit a chunk
 * never crosses. That single rule is what makes a retrieved passage's section title real
 * provenance rather than a guess: text filed under "Secondary Dominants" genuinely came
 * from under that heading.
 */
public final class SectionSplitter {

    private static final Pattern HEADING =
            Pattern.compile("<h([23])\\b[^>]*>(.*?)</h\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private SectionSplitter() {}

    public static List<HtmlSection> split(String html) {
        List<HtmlSection> sections = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return sections;
        }
        Matcher matcher = HEADING.matcher(html);
        int cursor = 0;
        String pendingHeading = "";
        int pendingLevel = 2;
        while (matcher.find()) {
            add(sections, pendingHeading, pendingLevel, html.substring(cursor, matcher.start()));
            pendingHeading = HtmlText.toText(matcher.group(2));
            pendingLevel = Integer.parseInt(matcher.group(1));
            cursor = matcher.end();
        }
        add(sections, pendingHeading, pendingLevel, html.substring(cursor));
        return sections;
    }

    private static void add(List<HtmlSection> sections, String heading, int level, String bodyHtml) {
        String text = HtmlText.toText(bodyHtml);
        if (text.isBlank()) {
            return;
        }
        sections.add(new HtmlSection(heading, level, text, sections.size()));
    }
}
