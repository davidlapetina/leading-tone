package fr.lapetina.music.knowledge.ingestion.omt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The parts of the Pressbooks REST API this ingester reads.
 *
 * <p>Everything ignores unknown properties: the publisher is free to add fields, and an
 * ingester that breaks when they do is an ingester that breaks silently one Tuesday.
 */
public final class PressbooksRecords {

    private PressbooksRecords() {}

    /** {@code /wp-json/pressbooks/v2/metadata} — the book, its authors and its licence. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookMetadata(
            String name,
            String alternativeHeadline,
            String copyrightYear,
            License license,
            List<Person> author,
            List<Person> editor) {

        public String version() {
            return alternativeHeadline == null ? "" : alternativeHeadline;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Person(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record License(String name, String url, String code) {}

    /** {@code /toc} — parts and chapters, each carrying its own licence and authorship. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Toc(List<Part> parts) {
        public Toc {
            parts = parts == null ? List.of() : List.copyOf(parts);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(long id, String title, String slug, List<Chapter> chapters) {
        public Part {
            chapters = chapters == null ? List.of() : List.copyOf(chapters);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chapter(
            long id,
            String title,
            String slug,
            String link,
            String status,
            @JsonProperty("word_count") int wordCount,
            @JsonProperty("has_post_content") boolean hasPostContent,
            ChapterMetadata metadata) {

        /** The chapter's own licence, which is not necessarily the book's. */
        public String licenseUrl() {
            return metadata == null || metadata.license() == null ? null : metadata.license().url();
        }

        public String licenseName() {
            return metadata == null || metadata.license() == null ? null : metadata.license().name();
        }

        public List<String> authorNames() {
            if (metadata == null || metadata.author() == null) {
                return List.of();
            }
            return metadata.author().stream().map(Person::name).filter(java.util.Objects::nonNull).toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChapterMetadata(String name, License license, List<Person> author, List<Person> editor) {}

    /** {@code /chapters/{id}} — the body, plus the licence again for cross-checking. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChapterContent(long id, Rendered title, Rendered content, String link) {

        public String html() {
            return content == null ? "" : content.rendered();
        }

        public String plainTitle() {
            return title == null ? "" : title.rendered();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rendered(String rendered) {}
}
