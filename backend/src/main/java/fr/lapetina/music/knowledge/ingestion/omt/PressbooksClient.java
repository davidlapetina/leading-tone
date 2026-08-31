package fr.lapetina.music.knowledge.ingestion.omt;

/**
 * Reads a Pressbooks book. Separate from the HTTP implementation so that ingestion can be
 * tested against recorded responses, including the awkward chapters that make the licence
 * gate worth having.
 */
public interface PressbooksClient {

    PressbooksRecords.BookMetadata metadata(String sourceId, String apiBase);

    PressbooksRecords.Toc toc(String sourceId, String apiBase);

    PressbooksRecords.ChapterContent chapter(String sourceId, String apiBase, long chapterId);
}
