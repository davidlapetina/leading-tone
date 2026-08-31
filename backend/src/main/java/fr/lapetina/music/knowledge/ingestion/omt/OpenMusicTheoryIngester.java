package fr.lapetina.music.knowledge.ingestion.omt;

import fr.lapetina.music.knowledge.ingestion.DocumentDraft;
import fr.lapetina.music.knowledge.ingestion.Ingester;
import fr.lapetina.music.knowledge.license.LicensePolicyService;
import fr.lapetina.music.knowledge.license.LicenseUrls;
import fr.lapetina.music.knowledge.source.SourceManifest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Reads Open Music Theory through the publisher's own API.
 *
 * <p>The important behaviour is the licence gate, and where it sits. Every chapter's
 * licence is read from the table of contents, and a chapter whose licence does not permit
 * ingestion <strong>is never requested</strong>. Its text does not reach this machine at
 * all, which is a stronger guarantee than downloading it and choosing not to use it.
 *
 * <p>This matters concretely: the book is CC BY-SA 4.0, but of its 140 chapters one is
 * CC BY-NC-SA 4.0 and one is All Rights Reserved. Trusting the book-level licence would
 * quietly ingest a chapter its author reserved.
 *
 * <p>The gate works from the licence URL, not from a list of chapter ids, so a new chapter
 * published tomorrow under terms nobody has considered is refused automatically.
 */
@ApplicationScoped
public class OpenMusicTheoryIngester implements Ingester {

    private static final Logger LOG = Logger.getLogger(OpenMusicTheoryIngester.class);
    private static final int PARSER_VERSION = 1;
    private static final int MIN_WORDS = 120;

    @Inject
    PressbooksClient client;

    @Inject
    LicensePolicyService licensePolicy;

    @Override
    public String sourceId() {
        return "open-music-theory";
    }

    @Override
    public int parserVersion() {
        return PARSER_VERSION;
    }

    @Override
    public Harvest harvest(SourceManifest.ManifestSource source) {
        String api = source.apiBase();
        PressbooksRecords.BookMetadata book = client.metadata(sourceId(), api);
        PressbooksRecords.Toc toc = client.toc(sourceId(), api);

        List<DocumentDraft> documents = new ArrayList<>();
        List<String> refused = new ArrayList<>();
        int seen = 0;
        int skippedEmpty = 0;

        for (PressbooksRecords.Part part : toc.parts()) {
            for (PressbooksRecords.Chapter chapter : part.chapters()) {
                seen++;
                if (!chapter.hasPostContent() || chapter.wordCount() < MIN_WORDS) {
                    skippedEmpty++;
                    continue;
                }
                String licenseUrl = chapter.licenseUrl();
                var identified = LicenseUrls.identify(licenseUrl);
                if (identified.isEmpty() || !licensePolicy.canIngestUrl(licenseUrl)) {
                    // Refused before the body is ever requested.
                    refused.add(chapter.title() + " (" + describe(chapter) + ")");
                    LOG.infof("Not ingesting \"%s\": licence is %s", chapter.title(), describe(chapter));
                    continue;
                }
                documents.add(fetch(source, book, part, chapter, identified.get()));
            }
        }
        LOG.infof("Open Music Theory: %d chapters seen, %d ingested, %d refused on licence, %d too short",
                seen, documents.size(), refused.size(), skippedEmpty);
        return new Harvest(documents, seen, skippedEmpty, refused);
    }

    private DocumentDraft fetch(SourceManifest.ManifestSource source,
                                PressbooksRecords.BookMetadata book,
                                PressbooksRecords.Part part,
                                PressbooksRecords.Chapter chapter,
                                String licenseId) {
        PressbooksRecords.ChapterContent content = client.chapter(sourceId(), source.apiBase(), chapter.id());
        List<String> authors = chapter.authorNames();
        return new DocumentDraft(
                Long.toString(chapter.id()),
                chapter.title(),
                part.title(),
                chapter.link(),
                authors,
                licenseId,
                attribution(book, chapter, authors, licenseId),
                content.html());
    }

    /**
     * Chapter authorship is preserved where the publisher gives it. Replacing a named
     * author with the book's author list would be a worse credit than none.
     */
    static String attribution(PressbooksRecords.BookMetadata book,
                              PressbooksRecords.Chapter chapter,
                              List<String> authors,
                              String licenseId) {
        StringBuilder credit = new StringBuilder();
        credit.append('"').append(chapter.title()).append("\", ");
        if (!authors.isEmpty()) {
            credit.append(String.join(", ", authors)).append(", ");
        }
        credit.append(book == null || book.name() == null ? "Open Music Theory" : book.name());
        if (book != null && !book.version().isBlank()) {
            credit.append(' ').append(book.version());
        }
        if (book != null && book.editor() != null && !book.editor().isEmpty()) {
            credit.append(", ed. ")
                    .append(String.join(", ", book.editor().stream().map(PressbooksRecords.Person::name).toList()));
        }
        return credit.append(". ").append(licenseId.replace("CC-", "CC ").replace("-4.0", " 4.0")).append('.').toString();
    }

    private static String describe(PressbooksRecords.Chapter chapter) {
        String name = chapter.licenseName();
        return name == null || name.isBlank() ? "not stated" : name;
    }
}
