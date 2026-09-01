package fr.lapetina.music.knowledge.harmony;

import fr.lapetina.music.knowledge.attribution.Attribution;
import fr.lapetina.music.knowledge.attribution.AttributionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds real musical examples by querying the annotations, not by similarity.
 *
 * <p>"Give me a Beethoven example of V/V" is a question with a right answer that either
 * exists in the corpus or does not. Answering it by embedding similarity would return
 * something that reads plausibly and might be about a different chord in a different piece.
 * So this is a query, and when it finds nothing it returns nothing — which the tutor is
 * required to report as "no verified example", never to paper over.
 */
@ApplicationScoped
public class HarmonySearchService {

    @Inject
    AttributionService attributionService;

    @Inject
    fr.lapetina.music.knowledge.license.LicensePolicyService licensePolicy;

    /**
     * @param romanNumeral matched as written, e.g. {@code V7/V}; null matches any
     * @param composer matched loosely, so "Beethoven" finds both Beethoven corpora
     */
    public List<MusicalExample> findExamples(String romanNumeral, String composer, String key, int limit) {
        return findExamples(romanNumeral, composer, key, null, limit);
    }

    /**
     * @param sourceIdPrefix restricts the search to one family of corpora, e.g. {@code dcml-}
     *     for the annotated scores. Without it the results are ordered by composer, which
     *     silently favours whichever corpus happens to contain the earliest name in the
     *     alphabet rather than the one that suits the question.
     */
    public List<MusicalExample> findExamples(String romanNumeral, String composer, String key,
                                             String sourceIdPrefix, int limit) {
        StringBuilder query = new StringBuilder("active = true");
        List<Object> parameters = new ArrayList<>();
        Set<String> allowed = retrievableSourceIds();
        if (allowed.isEmpty()) {
            return List.of();
        }
        if (romanNumeral != null && !romanNumeral.isBlank()) {
            parameters.add(romanNumeral.trim());
            query.append(" and romanNumeral = ?").append(parameters.size());
        }
        if (composer != null && !composer.isBlank()) {
            parameters.add("%" + composer.trim().toLowerCase(Locale.ROOT) + "%");
            query.append(" and lower(composer) like ?").append(parameters.size());
        }
        if (key != null && !key.isBlank()) {
            parameters.add("%" + key.trim().toLowerCase(Locale.ROOT) + "%");
            query.append(" and lower(globalKey) like ?").append(parameters.size());
        }
        if (sourceIdPrefix != null && !sourceIdPrefix.isBlank()) {
            parameters.add(sourceIdPrefix + "%");
            query.append(" and sourceId like ?").append(parameters.size());
        }
        parameters.add(allowed);
        query.append(" and sourceId in ?").append(parameters.size());

        return toExamples(HarmonyEvent.<HarmonyEvent>find(query.toString(), parameters.toArray())
                .page(0, Math.clamp(limit, 1, 50))
                .list());
    }

    /**
     * Examples of one harmony, each from a different piece.
     *
     * <p>Rows for a harmony arrive clustered: one movement can hold fifty tonic chords, so
     * asking for fifty gets fifty bars of the same movement. This asks which pieces contain
     * the harmony at all and then takes one from each, which is what makes two examples of a
     * triad two different pieces rather than two bars of Dvořák.
     */
    public List<MusicalExample> findExamplesAcrossWorks(String romanNumeral, String sourceIdPrefix,
                                                        int limit) {
        Set<String> allowed = retrievableSourceIds();
        if (allowed.isEmpty() || romanNumeral == null || romanNumeral.isBlank()) {
            return List.of();
        }
        String sourceFilter = sourceIdPrefix == null || sourceIdPrefix.isBlank()
                ? "%" : sourceIdPrefix + "%";
        List<String> works = HarmonyEvent.getEntityManager()
                .createQuery("select distinct e.work from HarmonyEvent e"
                        + " where e.active = true and e.romanNumeral = :numeral"
                        + " and e.sourceId like :source and e.sourceId in :allowed"
                        + " order by e.work", String.class)
                .setParameter("numeral", romanNumeral.trim())
                .setParameter("source", sourceFilter)
                .setParameter("allowed", allowed)
                .setMaxResults(Math.clamp(limit, 1, 20))
                .getResultList();

        List<HarmonyEvent> picked = new ArrayList<>();
        for (String work : works) {
            HarmonyEvent.<HarmonyEvent>find(
                            "active = true and romanNumeral = ?1 and work = ?2"
                                    + " and sourceId like ?3 and sourceId in ?4"
                                    + " order by measure, beat",
                            romanNumeral.trim(), work, sourceFilter, allowed)
                    .page(0, 1).firstResultOptional().ifPresent(picked::add);
        }
        return toExamples(picked);
    }

    public List<MusicalExample> findCadences(String cadenceType, String composer, int limit) {
        Set<String> allowed = retrievableSourceIds();
        if (allowed.isEmpty()) {
            return List.of();
        }
        String composerFilter = composer == null || composer.isBlank()
                ? "%" : "%" + composer.trim().toLowerCase(Locale.ROOT) + "%";
        return toExamples(HarmonyEvent.<HarmonyEvent>find(
                        "active = true and cadence = ?1 and lower(composer) like ?2 and sourceId in ?3",
                        cadenceType, composerFilter, allowed)
                .page(0, Math.clamp(limit, 1, 50))
                .list());
    }

    private List<MusicalExample> toExamples(List<HarmonyEvent> events) {
        return events.stream()
                .map(event -> MusicalExample.of(event, attributionService.forSource(event.sourceId)
                        .map(Attribution::shortCredit)
                        .orElse(event.sourceId)))
                .toList();
    }

    /**
     * Consecutive harmonies in one piece matching a pattern, e.g. {@code ii7 V7 Imaj7}.
     *
     * <p>A progression is a relationship between chords, not a label on one of them, so it
     * cannot be found by matching a single row. This walks forward from each candidate first
     * chord and checks that the next ones follow it in the same work.
     *
     * @return the first chord of each match, so the caller cites where the progression begins
     */
    public List<MusicalExample> findProgressions(List<String> pattern, String composer,
                                                String sourceIdPrefix, int limit) {
        if (pattern == null || pattern.size() < 2) {
            return List.of();
        }
        Set<String> allowed = retrievableSourceIds();
        if (allowed.isEmpty()) {
            return List.of();
        }
        List<HarmonyEvent> matches = new ArrayList<>();
        // Candidates are rows matching the first chord; each is then checked forwards. They
        // arrive ordered by work, and checking each one with its own query meant hundreds of
        // queries to answer one lesson -- eight seconds for a page. A work is loaded once
        // instead and every candidate in it is checked against what is already in hand.
        Map<String, List<HarmonyEvent>> loaded = new LinkedHashMap<>();
        for (HarmonyEvent start : candidates(pattern.get(0), composer, sourceIdPrefix, allowed, limit * 40)) {
            if (matches.size() >= limit) {
                break;
            }
            List<HarmonyEvent> inWork = loaded.computeIfAbsent(
                    start.sourceId + "\u0000" + start.work, key -> eventsOf(start));
            if (continues(start, inWork, pattern)) {
                matches.add(start);
            }
        }
        return toExamples(matches);
    }

    private List<HarmonyEvent> candidates(String first, String composer, String sourceIdPrefix,
                                          Set<String> allowed, int scan) {
        String composerFilter = composer == null || composer.isBlank()
                ? "%" : "%" + composer.trim().toLowerCase(Locale.ROOT) + "%";
        String sourceFilter = sourceIdPrefix == null || sourceIdPrefix.isBlank()
                ? "%" : sourceIdPrefix + "%";
        return HarmonyEvent.<HarmonyEvent>find(
                        "active = true and romanNumeral = ?1 and lower(composer) like ?2"
                                + " and sourceId like ?3 and sourceId in ?4"
                                + " order by work, measure, beat",
                        first, composerFilter, sourceFilter, allowed)
                .page(0, Math.clamp(scan, 1, 2000))
                .list();
    }

    /** Every annotation in one work, in order, so its candidates can be checked in memory. */
    private static List<HarmonyEvent> eventsOf(HarmonyEvent start) {
        return HarmonyEvent.<HarmonyEvent>find(
                        "active = true and sourceId = ?1 and work = ?2 order by measure, beat",
                        start.sourceId, start.work)
                .page(0, 4000)
                .list();
    }

    /** Whether the chords after this one, in the same work, complete the pattern. */
    private boolean continues(HarmonyEvent start, List<HarmonyEvent> inWork, List<String> pattern) {
        int from = 0;
        while (from < inWork.size() && inWork.get(from) != start) {
            from++;
        }
        List<HarmonyEvent> following = from < inWork.size()
                ? inWork.subList(from, Math.min(inWork.size(), from + pattern.size() * 8))
                : List.of();

        int expected = 0;
        for (HarmonyEvent event : following) {
            String numeral = event.romanNumeral;
            if (numeral == null) {
                continue;
            }
            if (numeral.equals(pattern.get(expected))) {
                expected++;
                if (expected == pattern.size()) {
                    return true;
                }
            } else if (expected > 0 && !numeral.equals(pattern.get(expected - 1))) {
                // A repeat of the chord we are on is fine; anything else breaks the run.
                return false;
            }
        }
        return false;
    }

    private Set<String> retrievableSourceIds() {
        return licensePolicy.retrievableSourceIds();
    }
}
