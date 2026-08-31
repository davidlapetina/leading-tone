package fr.lapetina.music.knowledge.retrieval;

/**
 * How the two searches are weighed against each other.
 *
 * <p>When there is no embedder the vector weight is zero and the lexical weight is one, so
 * pure BM25 falls out of the same arithmetic rather than needing a separate branch. That
 * is deliberate: a second code path is a second thing to get wrong.
 */
public record FusionWeights(double lexical, double vector, double conceptBoost, double takeawayBoost) {

    public static FusionWeights lexicalOnly(FusionWeights base) {
        return new FusionWeights(1.0, 0.0, base.conceptBoost(), base.takeawayBoost());
    }

    public static FusionWeights defaults() {
        return new FusionWeights(0.55, 0.45, 0.35, 0.20);
    }
}
