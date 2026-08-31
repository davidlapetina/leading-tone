package fr.lapetina.music.knowledge.ingestion.omt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Pressbooks client that reads recorded responses instead of the network, and remembers
 * which chapters were asked for.
 *
 * <p>The record of requests is the point: it lets a test assert that a chapter was never
 * fetched, which is a much stronger claim than asserting it was not used.
 */
final class FixturePressbooksClient implements PressbooksClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Long> requested = new ArrayList<>();

    List<Long> requestedChapters() {
        return List.copyOf(requested);
    }

    @Override
    public PressbooksRecords.BookMetadata metadata(String sourceId, String apiBase) {
        return read("metadata.json", PressbooksRecords.BookMetadata.class);
    }

    @Override
    public PressbooksRecords.Toc toc(String sourceId, String apiBase) {
        return read("toc.json", PressbooksRecords.Toc.class);
    }

    @Override
    public PressbooksRecords.ChapterContent chapter(String sourceId, String apiBase, long chapterId) {
        requested.add(chapterId);
        return read("chapter-" + chapterId + ".json", PressbooksRecords.ChapterContent.class);
    }

    private <T> T read(String name, Class<T> type) {
        String path = "knowledge/omt/" + name;
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing fixture " + path);
            }
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
