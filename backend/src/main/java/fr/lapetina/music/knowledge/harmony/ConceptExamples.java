package fr.lapetina.music.knowledge.harmony;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Finds real music that illustrates a concept, and engraves it.
 *
 * <p>The mapping from a concept to what to look for is deliberately narrow. A loose mapping
 * would always find <em>something</em>, and a passage that does not really show the thing
 * being taught is worse than an honest "no example loaded" — it teaches the learner to
 * distrust the citations that are good.
 */
@ApplicationScoped
public class ConceptExamples {

    private static final Logger LOG = Logger.getLogger(ConceptExamples.class);

    /** Source id prefixes: the annotated scores, and the jazz corpus. */
    private static final String SCORE_CORPORA = "dcml-";
    private static final String JAZZ_CORPUS = "jazz-";

    @org.eclipse.microprofile.config.inject.ConfigProperty(
            name = "music.knowledge.score.context-before", defaultValue = "1")
    int contextBefore;

    @org.eclipse.microprofile.config.inject.ConfigProperty(
            name = "music.knowledge.score.context-after", defaultValue = "1")
    int contextAfter;

    /**
     * Only concepts a Roman numeral can honestly stand for. A ii-V-I or a tritone
     * substitution is a pattern across several chords, not one label, so those are left out
     * rather than approximated by their first chord.
     */
    /**
     * The harmonies that illustrate a concept, in the corpus that concept is taught from.
     *
     * <p>Every numeral here was checked against the ingested corpora: a concept mapped to a
     * label nothing uses shows a learner an empty section, which reads as "there are no
     * examples of this in real music" and is a different claim from the truth. The classical
     * corpora and the jazz treebank do not share a vocabulary -- the treebank writes IM7 and
     * IVM7 where the scores write I and IV7 -- so the jazz concepts are listed in its terms.
     *
     * <p>What is deliberately absent is as considered as what is here. Notes, intervals,
     * scales and key signatures are not harmonic annotations, and counterpoint and altered
     * dominants are not labelled distinctly by any corpus we hold. Mapping them to the
     * nearest thing would put a bar under a heading it does not illustrate.
     */
    static final Map<String, List<String>> NUMERALS = Map.ofEntries(
            Map.entry("secondary-dominant", List.of("V7/V", "V/V", "V7/ii", "V/ii", "V7/IV", "V/IV", "V65/V")),
            Map.entry("dominant-seventh", List.of("V7", "V65", "V43", "V42")),
            Map.entry("dominant-function", List.of("V", "V7")),
            Map.entry("predominant-function", List.of("IV", "ii", "ii6")),
            Map.entry("tonic-function", List.of("I", "i")),
            Map.entry("roman-numeral", List.of("I", "IV", "V", "vi")),
            Map.entry("diatonic-triads", List.of("ii", "iii", "vi")),
            Map.entry("chord-inversion", List.of("I6", "V6", "V65", "I64")),
            Map.entry("seventh-chord", List.of("V7", "ii7", "vii°7")),
            Map.entry("modal-interchange", List.of("bVI", "bIII", "bVII", "iv")),
            // A triad in root position, which is what the concept is.
            Map.entry("triad", List.of("I", "IV", "V", "vi")),
            // Figured bass is these figures. Showing them is showing the notation itself.
            Map.entry("figured-bass", List.of("I6", "V65", "V43", "I64", "ii65")),
            Map.entry("harmonic-function", List.of("I", "IV", "V")),
            // Chords with a note above the seventh, in the treebank's spelling.
            Map.entry("extended-chord", List.of("IM7", "IVM7", "ii7", "vi7", "iii7")),
            // A voicing is built from these; the lead sheet names the chord and leaves the
            // rest to the player, which is the concept.
            Map.entry("jazz-voicing", List.of("IM7", "ii7", "V7", "vi7")),
            Map.entry("chord-symbol", List.of("IM7", "ii7", "V7", "I7")),
            // The same chord under two labels: the scores write bII7, the treebank bII.
            Map.entry("tritone-substitution", List.of("bII7", "bII")),
            Map.entry("chord-scale-theory", List.of("ii7", "V7", "IM7")));

    /** Concepts best shown by a cadence rather than a chord. */
    /**
     * Concepts that are a sequence of chords, not a chord.
     *
     * <p>For these there is no falling back to a single numeral: one chord out of the middle
     * of a progression is not an example of the progression, and showing it under the
     * concept's name says it is.
     */
    static final Set<String> IS_A_PROGRESSION = Set.of(
            "two-five-one", "turnaround", "chord-progression", "voice-leading");

    static final Map<String, String> CADENCES = Map.of(
            "cadence", "PAC",
            "modulation", "PAC");

    /**
     * Concepts that are a relationship between chords rather than a label on one.
     *
     * <p>A ii-V-I is not any of its chords; asking for "ii7" and showing the answer as a
     * two-five-one would be a different claim from the one being taught. These are matched
     * as consecutive runs instead, and the first chord of each run is cited.
     */
    static final Map<String, List<List<String>>> PROGRESSIONS = Map.of(
            "two-five-one", List.of(List.of("ii", "V", "I"), List.of("ii7", "V7", "I"),
                    List.of("ii6", "V", "I"), List.of("ii", "V7", "I")),
            "turnaround", List.of(List.of("I", "vi", "ii", "V"), List.of("I", "vi", "IV", "V"),
                    List.of("iii7", "vi7", "ii7", "V7"), List.of("IM7", "vi7", "ii7", "V7")),
            "chord-progression", List.of(List.of("I", "IV", "V"), List.of("IV", "V", "I")),
            "dominant-function", List.of(List.of("V", "I"), List.of("V7", "I")),
            "harmonic-function", List.of(List.of("I", "IV", "V"), List.of("I", "ii", "V")),
            // The whole of voice leading in two chords: the leading tone rises and the
            // seventh falls. Seeing it happen in a score is the lesson.
            "voice-leading", List.of(List.of("V7", "I"), List.of("V", "I"), List.of("V7", "i")));

    @Inject
    HarmonySearchService harmonySearch;

    @Inject
    fr.lapetina.music.concept.ConceptGraph conceptGraph;

    @Inject
    ScoreSource scoreSource;

    public List<MusicalExample> forConcept(String conceptId, int limit) {
        // A jazz concept is illustrated from the jazz corpus and a classical one from the
        // annotated scores. Searching everything at once would answer "show me a secondary
        // dominant" with a jazz standard, which is true but not what was being taught.
        // Written as a conditional rather than an Optional chain on purpose: mapping to null
        // inside an Optional yields an empty Optional, so the obvious chain here silently
        // sent jazz concepts to the classical corpora -- the exact opposite of the intent.
        boolean jazz = conceptGraph.find(conceptId)
                .map(fr.lapetina.music.concept.Concept::isJazz)
                .orElse(false);
        String corpus = jazz ? JAZZ_CORPUS : SCORE_CORPORA;

        // More than are shown, so the ones chosen can be spread across different pieces.
        int wanted = Math.min(limit * CANDIDATES_PER_RESULT, MOST_CANDIDATES);
        List<MusicalExample> found = new ArrayList<>();
        // Progressions are asked for narrowly. Each candidate costs a second query to check
        // what follows it, so asking for a wide pool of them turns a page load into half a
        // minute -- and unlike single chords they are already spread across pieces.
        int patternsWanted = limit * 3;
        for (List<String> pattern : PROGRESSIONS.getOrDefault(conceptId, List.of())) {
            if (found.size() >= patternsWanted) {
                break;
            }
            found.addAll(harmonySearch.findProgressions(pattern, null, corpus,
                    patternsWanted - found.size()));
        }
        // A concept that *is* a progression is shown as one or not at all. Falling back to a
        // single chord from the middle of it puts a lone iii7 under "Turnarounds" and a V7
        // from an unrelated tune under "The twelve-bar blues", which illustrates neither.
        if (!IS_A_PROGRESSION.contains(conceptId)) {
            // A slice from each numeral rather than filling up on the first. One numeral's
            // matches tend to come from one piece, so taking them all gives two bars of the
            // same movement where the concept has four different chords to show.
            List<String> numerals = NUMERALS.getOrDefault(conceptId, List.of());
            for (String numeral : numerals) {
                if (found.size() >= MOST_CANDIDATES) {
                    break;
                }
                found.addAll(harmonySearch.findExamplesAcrossWorks(numeral, corpus, limit + 2));
            }
        }
        if (found.isEmpty() && CADENCES.containsKey(conceptId)) {
            found.addAll(harmonySearch.findCadences(CADENCES.get(conceptId), null, wanted));
        }
        return preferringTheOnesWeCanDraw(found.stream()
                .filter(distinctBy(MusicalExample::sourceReference))
                .toList(), limit);
    }

    /**
     * Examples of a named harmony, optionally by a named composer.
     *
     * <p>Used when the learner said what they wanted. Asking for "a Beethoven example of
     * V/V" and being shown whatever illustrates the concept currently being taught is not
     * an answer to the question, and looking like one is worse than admitting the gap.
     */
    /**
     * How many matches to look at before choosing. More than are returned, because the ones
     * that can be engraved are worth finding and they are not necessarily the first.
     */
    private static final int CANDIDATES_PER_RESULT = 12;
    private static final int MOST_CANDIDATES = 60;

    public List<MusicalExample> forQuery(String romanNumeral, String cadence, String composer, int limit) {
        int wanted = Math.min(limit * CANDIDATES_PER_RESULT, MOST_CANDIDATES);
        List<MusicalExample> found = new ArrayList<>();
        if (cadence != null) {
            found.addAll(harmonySearch.findCadences(cadence, composer, wanted));
        } else if (romanNumeral != null) {
            // The annotated scores first, then the lead sheets. Both are real, but only the
            // scores come with bars, and the treebank is large enough and sorts early enough
            // that searching everything at once returned nothing else -- a query for V7/V
            // answered entirely with lead sheets while engraved Beethoven sat behind them.
            for (String corpus : List.of(SCORE_CORPORA, JAZZ_CORPUS)) {
                if (found.size() >= wanted) {
                    break;
                }
                found.addAll(harmonySearch.findExamples(
                        romanNumeral, composer, null, corpus, wanted - found.size()));
                // "V/V" and "V7/V" are the same request to a learner, and the corpus writes
                // both. These are the same harmony, not a loosening of the question.
                for (String variant : variantsOf(romanNumeral)) {
                    if (found.size() >= wanted) {
                        break;
                    }
                    found.addAll(harmonySearch.findExamples(
                            variant, composer, null, corpus, wanted - found.size()));
                }
            }
        }
        // Never widen further. If the harmony they named is not in the corpora, the answer is
        // that it is not there -- not the nearest thing by the same composer, which reads as
        // an answer and is not one.
        return preferringTheOnesWeCanDraw(found.stream()
                .filter(distinctBy(MusicalExample::sourceReference))
                .toList(), limit);
    }

    /**
     * The best {@code limit} of these, with the ones that come with bars first.
     *
     * <p>Not every corpus has note tables: the jazz treebank annotates chords over lead
     * sheets, so its examples are real but cannot be engraved. They also sort first
     * alphabetically, which meant a query for V7/V returned nothing but lead sheets while
     * engraved Beethoven sat behind them. Showing the music is the point of the feature, so
     * an example that can be drawn wins; one that cannot still beats an empty answer.
     */
    private List<MusicalExample> preferringTheOnesWeCanDraw(List<MusicalExample> candidates, int limit) {
        // Choose before engraving. Reading a note table is the expensive part, so the order
        // is decided first and only the examples actually shown are read.
        //
        // The ordering matters as much as the preference: matches for one harmony arrive
        // clustered by piece, so taking them as they come gives two bars of one movement
        // where the same harmony is available in three different hands.
        List<MusicalExample> firstOfItsWork = new ArrayList<>();
        List<MusicalExample> aWorkAlreadySeen = new ArrayList<>();
        Set<String> works = new LinkedHashSet<>();
        for (MusicalExample candidate : candidates) {
            (works.add(workOf(candidate)) ? firstOfItsWork : aWorkAlreadySeen).add(candidate);
        }
        List<MusicalExample> ordered = new ArrayList<>(firstOfItsWork);
        ordered.addAll(aWorkAlreadySeen);

        List<MusicalExample> drawn = new ArrayList<>();
        List<MusicalExample> undrawn = new ArrayList<>();
        for (MusicalExample candidate : ordered) {
            if (drawn.size() >= limit) {
                break;      // enough to show; no need to read any more note tables
            }
            MusicalExample engraved = engrave(candidate);
            if (engraved.abc() == null || engraved.abc().isBlank()) {
                undrawn.add(engraved);
            } else {
                drawn.add(engraved);
            }
        }
        for (MusicalExample rest : undrawn) {
            if (drawn.size() >= limit) {
                break;      // an example without bars still beats an empty answer
            }
            drawn.add(rest);
        }
        return List.copyOf(drawn);
    }

    private static String workOf(MusicalExample example) {
        String reference = example.sourceReference();
        if (reference == null) {
            return String.valueOf(example.work());
        }
        int comma = reference.indexOf(',');
        return comma < 0 ? reference : reference.substring(0, comma);
    }

    /** The same harmony written the other way. Not a different harmony. */
    private static List<String> variantsOf(String romanNumeral) {
        String base = romanNumeral.trim();
        int slash = base.indexOf('/');
        String head = slash < 0 ? base : base.substring(0, slash);
        String tail = slash < 0 ? "" : base.substring(slash);
        List<String> variants = new ArrayList<>();
        if (head.endsWith("7")) {
            variants.add(head.substring(0, head.length() - 1) + tail);
        } else {
            variants.add(head + "7" + tail);
            variants.add(head + "65" + tail);
        }
        variants.remove(base);
        return variants;
    }

    /** Two annotations of the same bar are one example, not two. */
    private static <T> java.util.function.Predicate<T> distinctBy(
            java.util.function.Function<T, Object> key) {
        java.util.Set<Object> seen = new java.util.HashSet<>();
        return value -> seen.add(key.apply(value));
    }

    /**
     * Attaches the notes of the cited bars, when the corpus publishes them.
     *
     * <p>A bar of context either side, because a chord shown alone is hard to hear in the
     * head — you need what it came from and what it went to. The target bar carries the
     * Roman numeral above the staff so the eye lands on the harmony being taught.
     */
    private MusicalExample engrave(MusicalExample example) {
        String abc = null;
        if (example.measure() != null) {
            int from = Math.max(1, example.measure() - contextBefore);
            int to = example.measure() + contextAfter;
            try {
                List<NoteEvent> notes = scoreSource.notesFor(
                        example.sourceId(), example.sourceReference(), from, to);
                if (!notes.isEmpty()) {
                    // No title in the notation: the card above it already carries the
                    // citation, and engraving it twice reads as a mistake.
                    abc = ScoreExcerptWriter.toAbc(notes, null, keySignatureOf(example), null,
                            from, to,
                            new ScoreExcerptWriter.Target(
                                    example.measure(), example.beat(), example.romanNumeral()));
                }
            } catch (RuntimeException e) {
                LOG.debugf("Could not engrave %s: %s", example.citation(), e.toString());
            }
        }
        return example.engravedAs(abc);
    }

    /** The tonic letter, which is what an ABC key field wants. */
    private static String keySignatureOf(MusicalExample example) {
        String key = example.globalKey();
        if (key == null || key.isBlank()) {
            return "C";
        }
        String[] parts = key.trim().split("\\s+");
        return parts.length > 1 && parts[1].startsWith("min") ? parts[0] + "m" : parts[0];
    }

}
