package fr.lapetina.music.learner;

/**
 * How a piece of knowledge was demonstrated, and how much that demonstration is worth.
 *
 * <p>Saying what V7 is counts for less than playing it, which counts for less than
 * explaining why its seventh has to fall.
 */
public enum EvidenceType {

    MULTIPLE_CHOICE(0.40, AnswerChannel.TEXT),
    HINTED_RECALL(0.30, AnswerChannel.TEXT),
    TEXT_RECALL(0.70, AnswerChannel.TEXT),
    AURAL_RECOGNITION(0.70, AnswerChannel.TEXT),
    MIDI_NOTE(0.70, AnswerChannel.MIDI),
    MIDI_INTERVAL(0.75, AnswerChannel.MIDI),
    MIDI_CHORD(0.80, AnswerChannel.MIDI),
    MIDI_SCALE(0.80, AnswerChannel.MIDI),
    MIDI_PROGRESSION(0.85, AnswerChannel.MIDI),
    SELF_EXPLANATION(0.85, AnswerChannel.TEXT),
    EXPLANATION(0.90, AnswerChannel.TEXT),
    TRANSFER_PROBLEM(1.00, AnswerChannel.TEXT);

    /** Evidence at or above this weight is what a claim of mastery has to rest on. */
    public static final double STRONG_THRESHOLD = 0.80;

    private final double weight;
    private final AnswerChannel channel;

    EvidenceType(double weight, AnswerChannel channel) {
        this.weight = weight;
        this.channel = channel;
    }

    public double weight() {
        return weight;
    }

    public AnswerChannel channel() {
        return channel;
    }

    public boolean isStrong() {
        return weight >= STRONG_THRESHOLD;
    }

    public enum AnswerChannel {
        TEXT,
        MIDI
    }
}
