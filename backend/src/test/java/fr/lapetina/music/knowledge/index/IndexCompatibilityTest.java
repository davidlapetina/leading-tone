package fr.lapetina.music.knowledge.index;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.knowledge.embedding.EmbeddingModelInfo;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Vectors made by one model are not comparable with vectors made by another, and an index
 * that quietly mixes them returns confident nonsense. The check is cheap; not having it is
 * the kind of bug that never announces itself.
 */
class IndexCompatibilityTest {

    private static final EmbeddingModelInfo BGE = new EmbeddingModelInfo("bge-small-en-v1.5-q", "1.5", 384);

    private static IndexMeta builtWith(EmbeddingModelInfo model) {
        return new IndexMeta(1, model.name(), model.version(), model.dimension(),
                MusicAnalyzer.ANALYZER_VERSION, 1, "10.4.0", Instant.now(), 1, 1);
    }

    @Test
    void acceptsAnIndexBuiltByTheSameModel() {
        assertTrue(builtWith(BGE).matches(BGE));
        assertTrue(builtWith(BGE).hasVectors());
    }

    @Test
    @DisplayName("a different model, version or dimension all make the vectors unusable")
    void refusesAnIndexBuiltByAnotherModel() {
        assertFalse(builtWith(new EmbeddingModelInfo("all-minilm-l6-v2", "1.0", 384)).matches(BGE),
                "same dimension, different model: the numbers mean different things");
        assertFalse(builtWith(new EmbeddingModelInfo("bge-small-en-v1.5-q", "2.0", 384)).matches(BGE),
                "a retrained model of the same name is still a different model");
        assertFalse(builtWith(new EmbeddingModelInfo("bge-small-en-v1.5-q", "1.5", 768)).matches(BGE));
    }

    @Test
    @DisplayName("an index with no vectors is searched lexically rather than refused")
    void treatsAVectorlessIndexAsLexicalOnly() {
        IndexMeta lexical = builtWith(EmbeddingModelInfo.NONE);

        assertFalse(lexical.hasVectors());
        assertFalse(lexical.matches(BGE), "there is nothing to compare against");
    }

    @Test
    void signatureIsWhatIdentifiesAModel() {
        assertTrue(BGE.signature().contains("384"));
        assertFalse(EmbeddingModelInfo.NONE.isPresent());
    }
}
