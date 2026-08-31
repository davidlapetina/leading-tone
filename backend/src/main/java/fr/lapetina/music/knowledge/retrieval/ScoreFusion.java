package fr.lapetina.music.knowledge.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines a lexical ranking and a vector ranking into one.
 *
 * <p>The formula, in full, because a ranking nobody can explain is a ranking nobody can
 * fix:
 *
 * <pre>
 *   normalise each list by its own best score, so the top hit is 1.0
 *   base(c)  = wLex * lex(c) + wVec * vec(c)        a missing side contributes 0
 *   score(c) = base(c) * (1 + conceptBoost + takeawayBoost)
 *   dedup by chunk id, keeping the higher score
 *   sort by score, then by document and position so ties are stable
 *   allow at most maxPerDocument chunks from one document
 * </pre>
 *
 * <p>Normalising at all, rather than adding raw scores, because BM25 is unbounded and
 * corpus-dependent while a cosine sits in a fixed range; adding them directly compares two
 * things measured in different units.
 *
 * <p>Dividing by the best score rather than stretching the list across [0,1]. Min-max is
 * the more obvious choice and it is wrong here for two reasons: it forces the worst
 * candidate to exactly zero, where no boost can ever reach it however relevant it is; and
 * on a short list it throws away how close the scores were, so a run-away winner and a
 * photo finish come out identical. Dividing by the maximum keeps both.
 *
 * <p>Deliberately free of Lucene and CDI so the ranking can be tested by handing it lists
 * of numbers.
 */
public final class ScoreFusion {

    /** One result from one of the two searches. */
    public record Candidate(String chunkId, String documentId, long order, double score) {}

    /** A combined result, keeping both inputs so a ranking can be explained. */
    public record Fused(String chunkId, String documentId, long order,
                        double score, double lexical, double vector, double boost) {}

    private ScoreFusion() {}

    public static List<Fused> fuse(List<Candidate> lexical,
                                   List<Candidate> vector,
                                   FusionWeights weights,
                                   Map<String, Double> boosts,
                                   int maxPerDocument,
                                   int limit) {
        Map<String, Double> lexicalScores = normalise(lexical);
        Map<String, Double> vectorScores = normalise(vector);

        Map<String, Candidate> all = new LinkedHashMap<>();
        lexical.forEach(candidate -> all.putIfAbsent(candidate.chunkId(), candidate));
        vector.forEach(candidate -> all.putIfAbsent(candidate.chunkId(), candidate));

        List<Fused> fused = new ArrayList<>(all.size());
        for (Candidate candidate : all.values()) {
            double lex = lexicalScores.getOrDefault(candidate.chunkId(), 0.0);
            double vec = vectorScores.getOrDefault(candidate.chunkId(), 0.0);
            double base = weights.lexical() * lex + weights.vector() * vec;
            double boost = boosts == null ? 0.0 : boosts.getOrDefault(candidate.chunkId(), 0.0);
            fused.add(new Fused(candidate.chunkId(), candidate.documentId(), candidate.order(),
                    base * (1.0 + boost), lex, vec, boost));
        }

        fused.sort(Comparator.comparingDouble(Fused::score).reversed()
                .thenComparing(Fused::documentId)
                .thenComparingLong(Fused::order)
                .thenComparing(Fused::chunkId));

        return capPerDocument(fused, maxPerDocument, limit);
    }

    /**
     * Scales one list so its best hit is 1.0 and the rest keep their proportions.
     *
     * <p>An all-equal list normalises to 1.0 throughout: with nothing to distinguish the
     * entries, the honest answer is that they are equally good, not that they are all
     * worthless. A list whose scores are all zero or negative normalises to 1.0 as well,
     * for the same reason — a search returned them, which is itself weak evidence.
     */
    static Map<String, Double> normalise(List<Candidate> candidates) {
        Map<String, Double> normalised = new HashMap<>();
        if (candidates == null || candidates.isEmpty()) {
            return normalised;
        }
        double max = candidates.stream().mapToDouble(Candidate::score).max().orElse(0.0);
        for (Candidate candidate : candidates) {
            normalised.merge(candidate.chunkId(),
                    max > 0 ? candidate.score() / max : 1.0, Math::max);
        }
        return normalised;
    }

    /** Stops one chapter monopolising the answer when several of its passages match. */
    static List<Fused> capPerDocument(List<Fused> sorted, int maxPerDocument, int limit) {
        List<Fused> kept = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (Fused candidate : sorted) {
            if (kept.size() >= limit) {
                break;
            }
            int used = seen.getOrDefault(candidate.documentId(), 0);
            if (maxPerDocument > 0 && used >= maxPerDocument) {
                continue;
            }
            seen.put(candidate.documentId(), used + 1);
            kept.add(candidate);
        }
        return kept;
    }
}
