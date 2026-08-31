package fr.lapetina.music.knowledge.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The ranking is a pure function over numbers, so it is tested by handing it numbers. A
 * ranking that can only be observed through a live index is a ranking nobody will dare
 * change.
 */
class ScoreFusionTest {

    private static ScoreFusion.Candidate candidate(String id, double score) {
        return new ScoreFusion.Candidate(id, "doc-" + id, 0, score);
    }

    private static List<String> ids(List<ScoreFusion.Fused> fused) {
        return fused.stream().map(ScoreFusion.Fused::chunkId).toList();
    }

    private static final FusionWeights EVEN = new FusionWeights(0.5, 0.5, 0.35, 0.20);

    @Test
    @DisplayName("with no embedder the vector weight is zero and the lexical order survives intact")
    void fallsBackToLexicalOrderWithoutVectors() {
        List<ScoreFusion.Candidate> lexical =
                List.of(candidate("a", 9.0), candidate("b", 4.0), candidate("c", 1.0));

        List<ScoreFusion.Fused> fused = ScoreFusion.fuse(
                lexical, List.of(), FusionWeights.lexicalOnly(EVEN), Map.of(), 2, 10);

        assertEquals(List.of("a", "b", "c"), ids(fused));
    }

    @Test
    void combinesBothRankings() {
        List<ScoreFusion.Candidate> lexical = List.of(candidate("a", 10.0), candidate("b", 1.0));
        List<ScoreFusion.Candidate> vector = List.of(candidate("b", 0.9), candidate("a", 0.1));

        List<ScoreFusion.Fused> fused = ScoreFusion.fuse(lexical, vector, EVEN, Map.of(), 2, 10);

        assertEquals(2, fused.size());
        for (ScoreFusion.Fused hit : fused) {
            assertTrue(hit.lexical() > 0.0 && hit.vector() > 0.0,
                    "a passage both searches found should carry credit from both");
        }
    }

    @Test
    @DisplayName("a passage found by only one search keeps that search's credit and no other")
    void doesNotInventAScoreForAMissingSide() {
        List<ScoreFusion.Fused> fused = ScoreFusion.fuse(
                List.of(candidate("only-lexical", 5.0)), List.of(), EVEN, Map.of(), 2, 10);

        assertEquals(0.0, fused.get(0).vector(), 1e-9);
        assertTrue(fused.get(0).lexical() > 0.0);
    }

    @Test
    @DisplayName("scaling keeps how close the scores were, which min-max would flatten")
    void preservesRelativeDistance() {
        Map<String, Double> close = ScoreFusion.normalise(
                List.of(candidate("a", 10.0), candidate("b", 9.5)));
        Map<String, Double> far = ScoreFusion.normalise(
                List.of(candidate("a", 10.0), candidate("b", 1.0)));

        assertTrue(close.get("b") > far.get("b"),
                "a near miss must not score the same as a distant one");
    }

    @Test
    @DisplayName("when nothing distinguishes the candidates they are equally good, not equally bad")
    void normalisesAnAllEqualListToOne() {
        Map<String, Double> normalised = ScoreFusion.normalise(
                List.of(candidate("a", 3.0), candidate("b", 3.0)));

        assertEquals(1.0, normalised.get("a"), 1e-9);
        assertEquals(1.0, normalised.get("b"), 1e-9);
    }

    @Test
    void handlesEmptyInput() {
        assertTrue(ScoreFusion.normalise(List.of()).isEmpty());
        assertTrue(ScoreFusion.fuse(List.of(), List.of(), EVEN, Map.of(), 2, 10).isEmpty());
    }

    @Test
    @DisplayName("being about the concept under discussion can lift a passage past a better word match")
    void conceptBoostChangesTheOrder() {
        List<ScoreFusion.Candidate> lexical = List.of(candidate("generic", 10.0), candidate("onTopic", 8.0));

        assertEquals(List.of("generic", "onTopic"),
                ids(ScoreFusion.fuse(lexical, List.of(), FusionWeights.lexicalOnly(EVEN), Map.of(), 2, 10)));
        assertEquals(List.of("onTopic", "generic"),
                ids(ScoreFusion.fuse(lexical, List.of(), FusionWeights.lexicalOnly(EVEN),
                        Map.of("onTopic", 0.5), 2, 10)));
    }

    @Test
    @DisplayName("one chapter cannot monopolise the answer")
    void capsChunksPerDocument() {
        List<ScoreFusion.Candidate> lexical = List.of(
                new ScoreFusion.Candidate("a1", "same", 1, 10.0),
                new ScoreFusion.Candidate("a2", "same", 2, 9.0),
                new ScoreFusion.Candidate("a3", "same", 3, 8.0),
                new ScoreFusion.Candidate("b1", "other", 1, 7.0));

        assertEquals(List.of("a1", "a2", "b1"),
                ids(ScoreFusion.fuse(lexical, List.of(), FusionWeights.lexicalOnly(EVEN), Map.of(), 2, 10)));
    }

    @Test
    void deduplicatesByChunkId() {
        List<ScoreFusion.Fused> fused = ScoreFusion.fuse(
                List.of(candidate("same", 5.0)), List.of(candidate("same", 0.5)), EVEN, Map.of(), 5, 10);

        assertEquals(1, fused.size());
    }

    @Test
    @DisplayName("ties break the same way every time, whatever order the searches returned")
    void isDeterministicUnderTies() {
        List<ScoreFusion.Candidate> tied = new ArrayList<>(List.of(
                new ScoreFusion.Candidate("a", "doc", 1, 5.0),
                new ScoreFusion.Candidate("b", "doc", 2, 5.0),
                new ScoreFusion.Candidate("c", "doc", 3, 5.0)));

        List<String> expected = null;
        for (int shuffle = 0; shuffle < 25; shuffle++) {
            Collections.shuffle(tied);
            List<String> actual = ids(ScoreFusion.fuse(
                    tied, List.of(), FusionWeights.lexicalOnly(EVEN), Map.of(), 10, 10));
            if (expected == null) {
                expected = actual;
            }
            assertEquals(expected, actual);
        }
    }
}
