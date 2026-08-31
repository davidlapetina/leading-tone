package fr.lapetina.music.tutor;

import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Works out which concept, if any, the learner just asked about.
 *
 * <p>A tutor that ploughs on with its own plan while the student asks something else is
 * not a tutor. This is deliberately a plain keyword match rather than a model call: it
 * decides where the conversation goes, so it belongs with the rest of the deterministic
 * policy, and it is cheap enough to run on every message.
 */
@ApplicationScoped
public class FocusDetector {

    /**
     * Aliases beyond each concept's own name. Ordered longest-first at match time, so
     * "dominant seventh" wins over "dominant" and "seventh".
     */
    private static final Map<String, List<String>> ALIASES = buildAliases();

    @Inject
    ConceptGraph conceptGraph;

    private static Map<String, List<String>> buildAliases() {
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        aliases.put("note", List.of("note", "notes", "sharp", "flat", "enharmonic", "black key", "white key"));
        aliases.put("interval", List.of("interval", "intervals", "tritone", "major third", "minor third",
                "perfect fifth", "octave"));
        aliases.put("major-scale", List.of("major scale", "scale"));
        aliases.put("minor-scale", List.of("minor scale", "harmonic minor", "melodic minor", "natural minor"));
        aliases.put("key-signature", List.of("key signature", "key signatures", "circle of fifths",
                "how many sharps", "how many flats"));
        aliases.put("scale-degree", List.of("scale degree", "scale degrees", "supertonic", "mediant",
                "submediant", "leading tone", "subdominant"));
        aliases.put("mode", List.of("mode", "modes", "dorian", "phrygian", "lydian", "mixolydian", "locrian",
                "aeolian", "ionian"));
        aliases.put("triad", List.of("triad", "triads", "major chord", "minor chord", "diminished chord",
                "augmented chord"));
        aliases.put("chord-inversion", List.of("inversion", "inversions", "inverted", "first inversion",
                "second inversion", "slash chord", "bass note"));
        aliases.put("diatonic-triads", List.of("diatonic triad", "diatonic triads", "diatonic chord",
                "chords in a key", "diatonic"));
        aliases.put("seventh-chord", List.of("seventh chord", "seventh chords", "add 7", "add7", "maj7",
                "major seventh", "minor seventh", "half diminished", "7th chord", "seventh", "7th"));
        aliases.put("roman-numeral", List.of("roman numeral", "roman numerals"));
        aliases.put("figured-bass", List.of("figured bass", "figures"));
        aliases.put("harmonic-function", List.of("harmonic function", "function of a chord"));
        aliases.put("tonic-function", List.of("tonic function", "tonic"));
        aliases.put("predominant-function", List.of("predominant", "pre dominant"));
        aliases.put("dominant-function", List.of("dominant function", "dominant"));
        aliases.put("dominant-seventh", List.of("dominant seventh", "dominant 7th", "v7"));
        aliases.put("cadence", List.of("cadence", "cadences", "authentic cadence", "plagal", "deceptive",
                "half cadence"));
        aliases.put("voice-leading", List.of("voice leading", "parallel fifths", "parallel octaves",
                "tendency tone", "resolve", "resolution"));
        aliases.put("secondary-dominant", List.of("secondary dominant", "secondary dominants", "applied dominant",
                "v/v", "v7/v", "tonicise", "tonicize"));
        aliases.put("modulation", List.of("modulation", "modulate", "change key", "pivot chord", "pivot"));
        return Map.copyOf(aliases);
    }

    /** The longest recognised phrase in the message, or empty when nothing is recognised. */
    public Optional<LearnerFocus> detect(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        String text = " " + message.toLowerCase().replaceAll("[^a-z0-9/ ]", " ").replaceAll("\\s+", " ") + " ";

        record Candidate(String conceptId, String phrase) {
        }
        List<Candidate> matches = new ArrayList<>();

        for (Concept concept : conceptGraph.all()) {
            List<String> phrases = new ArrayList<>(ALIASES.getOrDefault(concept.id(), List.of()));
            phrases.add(concept.name().toLowerCase());
            phrases.add(concept.id().replace('-', ' '));
            for (String phrase : phrases) {
                if (text.contains(" " + phrase + " ")) {
                    matches.add(new Candidate(concept.id(), phrase));
                }
            }
        }
        return matches.stream()
                .max((left, right) -> Integer.compare(left.phrase().length(), right.phrase().length()))
                .map(match -> new LearnerFocus(match.conceptId(), match.phrase()));
    }
}
