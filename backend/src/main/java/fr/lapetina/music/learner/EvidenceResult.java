package fr.lapetina.music.learner;

public enum EvidenceResult {

    CORRECT(1.0),
    PARTIALLY_CORRECT(0.5),
    INCORRECT(0.0),
    /** The learner declined to answer: worth recording, but it moves mastery very little. */
    SKIPPED(0.0);

    private final double correctness;

    EvidenceResult(double correctness) {
        this.correctness = correctness;
    }

    public double correctness() {
        return correctness;
    }

    public boolean isPositive() {
        return correctness > 0;
    }
}
