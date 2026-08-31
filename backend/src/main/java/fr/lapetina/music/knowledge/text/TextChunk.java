package fr.lapetina.music.knowledge.text;

import fr.lapetina.music.knowledge.chunk.ChunkKind;

/** A passage produced by the chunker, before it is given provenance and persisted. */
public record TextChunk(String sectionTitle, int sectionOrder, int chunkOrder, ChunkKind kind, String body) {

    public int wordCount() {
        return body.isBlank() ? 0 : body.trim().split("\\s+").length;
    }
}
