package fr.lapetina.music.knowledge.ingestion.omt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lapetina.music.knowledge.ingestion.SourceFetcher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** The Pressbooks client that actually talks to the publisher. */
@ApplicationScoped
public class HttpPressbooksClient implements PressbooksClient {

    @Inject
    SourceFetcher fetcher;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public PressbooksRecords.BookMetadata metadata(String sourceId, String apiBase) {
        return read(sourceId, apiBase + "/metadata", PressbooksRecords.BookMetadata.class);
    }

    @Override
    public PressbooksRecords.Toc toc(String sourceId, String apiBase) {
        return read(sourceId, apiBase + "/toc", PressbooksRecords.Toc.class);
    }

    @Override
    public PressbooksRecords.ChapterContent chapter(String sourceId, String apiBase, long chapterId) {
        return read(sourceId, apiBase + "/chapters/" + chapterId, PressbooksRecords.ChapterContent.class);
    }

    private <T> T read(String sourceId, String url, Class<T> type) {
        String body = fetcher.get(sourceId, url);
        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read " + url + ": " + e.getOriginalMessage(), e);
        }
    }
}
