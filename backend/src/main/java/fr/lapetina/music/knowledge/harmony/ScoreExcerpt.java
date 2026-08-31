package fr.lapetina.music.knowledge.harmony;

import java.util.List;

/**
 * A few bars of real music, ready to engrave.
 *
 * <p>{@code abc} is what the browser draws. The citation fields travel with it because an
 * excerpt without its bar numbers is not evidence of anything.
 */
public record ScoreExcerpt(
        String composer,
        String work,
        String movement,
        int fromMeasure,
        int toMeasure,
        String keySignature,
        String timeSignature,
        String abc,
        List<String> highlighted,
        String attribution,
        String licenseId,
        ExampleOrigin origin) {

    public String citation() {
        StringBuilder text = new StringBuilder(composer).append(", ").append(work);
        if (movement != null && !movement.isBlank()) {
            text.append(", ").append(movement);
        }
        text.append(fromMeasure == toMeasure ? ", bar " + fromMeasure : ", bars " + fromMeasure + "-" + toMeasure);
        return text.toString();
    }
}
