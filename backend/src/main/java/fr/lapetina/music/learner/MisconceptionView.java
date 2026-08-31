package fr.lapetina.music.learner;

import java.time.Instant;

public record MisconceptionView(String conceptId, String code, String description, int occurrences,
                                Instant lastSeenAt) {

    public static MisconceptionView of(Misconception misconception) {
        return new MisconceptionView(misconception.conceptId, misconception.code, misconception.description,
                misconception.occurrences, misconception.lastSeenAt);
    }
}
