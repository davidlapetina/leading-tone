package fr.lapetina.music.knowledge.retrieval;

import java.util.List;

/**
 * What retrieval found, and how.
 *
 * <p>{@code vectorUsed} is reported rather than assumed: an index built before an
 * embedding model was configured is searched lexically, and the caller should be able to
 * tell that from the result rather than from a log line.
 */
public record RetrievalResult(List<RetrievedChunk> chunks, boolean vectorUsed, long millis) {

    public static final RetrievalResult EMPTY = new RetrievalResult(List.of(), false, 0);

    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    public List<String> chunkIds() {
        return chunks.stream().map(RetrievedChunk::chunkId).toList();
    }

    public List<String> sourceIds() {
        return chunks.stream().map(RetrievedChunk::sourceId).distinct().toList();
    }
}
