package fr.lapetina.music.knowledge.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns published HTML into plain text, keeping the structure that matters for teaching.
 *
 * <p>Deliberately hand-written rather than a general HTML library. The input is
 * Pressbooks-generated markup from one known publisher, not the open web; the output is
 * fenced as untrusted quotation either way; and an uber-jar we are already keeping honest
 * about its size does not need another dependency for a job this narrow.
 *
 * <p>The music-specific part is the entity table: {@code &#9837;} must survive as a flat
 * sign, because "b" and "♭" are not the same character to a reader and the difference
 * between B and B♭ is the whole subject.
 */
public final class HtmlText {

    /** Elements whose entire contents are dropped, not just their tags. */
    private static final List<String> DROPPED = List.of(
            "script", "style", "iframe", "noscript", "svg", "form", "audio", "video", "figcaption");

    private static final Map<String, String> ENTITIES = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"),
            Map.entry("quot", "\""), Map.entry("apos", "'"), Map.entry("nbsp", " "),
            Map.entry("hellip", "…"), Map.entry("mdash", "—"), Map.entry("ndash", "–"),
            Map.entry("rsquo", "’"), Map.entry("lsquo", "‘"),
            Map.entry("ldquo", "“"), Map.entry("rdquo", "”"),
            Map.entry("flat", "♭"), Map.entry("sharp", "♯"), Map.entry("natural", "♮"));

    private HtmlText() {}

    /** Plain text, with block structure preserved as blank lines and list items as "- ". */
    public static String toText(String html) {
        return collapse(unLatex(decode(strip(html))));
    }

    private static String strip(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(html.length());
        int i = 0;
        while (i < html.length()) {
            char c = html.charAt(i);
            if (c != '<') {
                out.append(c);
                i++;
                continue;
            }
            int close = html.indexOf('>', i);
            if (close < 0) {
                out.append(html.substring(i));
                break;
            }
            String tag = html.substring(i + 1, close).trim();
            String name = tagName(tag);
            if (DROPPED.contains(name) && !tag.startsWith("/") && !tag.endsWith("/")) {
                int end = skipElement(html, close + 1, name);
                i = end;
                continue;
            }
            out.append(spacingFor(name, tag));
            i = close + 1;
        }
        return out.toString();
    }

    /** Walks to the matching close tag, counting nesting so an inner copy does not end it early. */
    private static int skipElement(String html, int from, String name) {
        int depth = 1;
        int i = from;
        while (i < html.length() && depth > 0) {
            int open = html.indexOf('<', i);
            if (open < 0) {
                return html.length();
            }
            int close = html.indexOf('>', open);
            if (close < 0) {
                return html.length();
            }
            String tag = html.substring(open + 1, close).trim();
            if (tagName(tag).equals(name)) {
                depth += tag.startsWith("/") ? -1 : (tag.endsWith("/") ? 0 : 1);
            }
            i = close + 1;
        }
        return i;
    }

    private static String tagName(String tag) {
        int end = 0;
        int start = tag.startsWith("/") ? 1 : 0;
        while (end + start < tag.length() && Character.isLetterOrDigit(tag.charAt(start + end))) {
            end++;
        }
        return tag.substring(start, start + end).toLowerCase(Locale.ROOT);
    }

    private static String spacingFor(String name, String tag) {
        return switch (name) {
            case "p", "div", "br", "tr", "h1", "h2", "h3", "h4", "h5", "h6",
                    "ul", "ol", "table", "blockquote", "section" -> "\n\n";
            case "li" -> tag.startsWith("/") ? "\n" : "\n- ";
            case "td", "th" -> " ";
            case "img" -> altText(tag);
            default -> "";
        };
    }

    /** An image becomes its alt text, or nothing. A figure we cannot describe is not quoted. */
    private static String altText(String tag) {
        int at = tag.toLowerCase(Locale.ROOT).indexOf("alt=");
        if (at < 0) {
            return "";
        }
        int q = at + 4;
        if (q >= tag.length()) {
            return "";
        }
        char quote = tag.charAt(q);
        if (quote != '"' && quote != '\'') {
            return "";
        }
        int end = tag.indexOf(quote, q + 1);
        String alt = end < 0 ? "" : tag.substring(q + 1, end).trim();
        return alt.isEmpty() ? "" : " [figure: " + alt + "] ";
    }

    static String decode(String text) {
        if (text.indexOf('&') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            int semi = text.indexOf(';', i);
            if (semi < 0 || semi - i > 10) {
                out.append(c);
                i++;
                continue;
            }
            String body = text.substring(i + 1, semi);
            String replacement = resolve(body);
            if (replacement == null) {
                out.append(c);
                i++;
            } else {
                out.append(replacement);
                i = semi + 1;
            }
        }
        return out.toString();
    }

    private static String resolve(String body) {
        if (body.startsWith("#")) {
            try {
                int code = body.startsWith("#x") || body.startsWith("#X")
                        ? Integer.parseInt(body.substring(2), 16)
                        : Integer.parseInt(body.substring(1));
                return Character.toString(code);
            } catch (NumberFormatException notANumber) {
                return null;
            }
        }
        return ENTITIES.get(body.toLowerCase(Locale.ROOT));
    }

    /**
     * Open Music Theory writes scale-degree carets and accidentals as LaTeX shortcodes.
     * Left alone they reach the learner as "[latex](\\downarrow\\hat3[/latex]", which is
     * worse than useless in a citation. The notation they stand for is meaningful, so it is
     * translated rather than deleted.
     */
    static String unLatex(String text) {
        if (!text.contains("[latex]") && !text.contains("\\hat")) {
            return text;
        }
        return text
                .replace("[latex]", "")
                .replace("[/latex]", "")
                .replaceAll("\\\\downarrow\\s*", "\u2193")
                .replaceAll("\\\\uparrow\\s*", "\u2191")
                .replaceAll("\\\\flat\\s*", "\u266d")
                .replaceAll("\\\\sharp\\s*", "\u266f")
                .replaceAll("\\\\natural\\s*", "\u266e")
                .replaceAll("\\\\hat\\{?(\\d)\\}?", "$1\u0302")
                .replaceAll("\\\\[a-zA-Z]+", "")
                .replaceAll("[{}]", "")
                .replaceAll("\\s{2,}", " ");
    }

    private static String collapse(String text) {
        List<String> lines = new ArrayList<>();
        for (String raw : text.split("\n")) {
            String line = raw.replace(' ', ' ').replaceAll("[ \t\f\r]+", " ").trim();
            if (!line.isEmpty() || (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty())) {
                lines.add(line);
            }
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines).trim();
    }
}
