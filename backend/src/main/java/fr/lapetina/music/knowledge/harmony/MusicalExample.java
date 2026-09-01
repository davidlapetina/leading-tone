package fr.lapetina.music.knowledge.harmony;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * One citable moment in a piece of music: what happens, where, and how it is engraved.
 *
 * <p>The measure number is the point. "Beethoven uses this" is a claim nobody can check;
 * "Beethoven, Sonata no. 2, bar 17" is a claim anybody can. {@code eventId} is the row it
 * came from, so a citation in an answer can be traced back to the annotation that produced
 * it rather than matched by its printed text.
 *
 * <p>{@code abc} is the engraved passage, or null when the corpus publishes annotations but
 * no notes. Null means "no score available", never "here is one we made up".
 */
public record MusicalExample(
        UUID eventId,
        String sourceId,
        ExampleOrigin origin,
        String composer,
        String work,
        String movement,
        Integer measure,
        Double beat,
        String globalKey,
        String localKey,
        String romanNumeral,
        String cadence,
        String sourceReference,
        String abc,
        String licenseId,
        String attribution) {

    public static MusicalExample of(HarmonyEvent event, String attribution) {
        return new MusicalExample(event.id, event.sourceId, ExampleOrigin.VERIFIED_CORPUS,
                event.composer, event.work, event.movement, event.measure, event.beat,
                event.globalKey, event.localKey, event.romanNumeral, event.cadence,
                event.sourceReference, null, event.licenseId, attribution);
    }

    /** The same example with its notation attached. */
    public MusicalExample engravedAs(String abc) {
        return new MusicalExample(eventId, sourceId, origin, composer, work, movement, measure,
                beat, globalKey, localKey, romanNumeral, cadence, sourceReference, abc,
                licenseId, attribution);
    }

    /**
     * How a citation reads to a learner.
     *
     * <p>Annotated because Jackson serialises a record's components, not its methods, and
     * this is part of the API contract: the interface validates the shape it receives and
     * renders nothing if a field it needs has quietly gone.
     */
    @JsonProperty("citation")
    public String citation() {
        StringBuilder text = new StringBuilder(composer == null ? "" : composer);
        if (work != null) {
            text.append(text.isEmpty() ? "" : ", ").append(work);
        }
        if (movement != null && !movement.isBlank()) {
            text.append(", ").append(movement);
        }
        if (measure != null) {
            text.append(", bar ").append(measure);
        }
        return text.toString();
    }

    @JsonProperty("hasScore")
    public boolean hasScore() {
        return abc != null && !abc.isBlank();
    }
}
