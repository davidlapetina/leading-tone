package fr.lapetina.music.knowledge.router;

import fr.lapetina.music.knowledge.attribution.Attribution;
import fr.lapetina.music.knowledge.attribution.AttributionService;
import fr.lapetina.music.knowledge.harmony.ConceptExamples;
import fr.lapetina.music.knowledge.harmony.MusicalExample;
import fr.lapetina.music.knowledge.retrieval.KnowledgeRetriever;
import fr.lapetina.music.knowledge.retrieval.RetrievalQuery;
import fr.lapetina.music.knowledge.retrieval.RetrievalResult;
import fr.lapetina.music.knowledge.retrieval.RetrievedChunk;
import fr.lapetina.music.settings.SettingsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Decides where a question's answer comes from, and gathers it.
 *
 * <p>Three rules, applied in this order:
 *
 * <ol>
 *   <li><strong>Deterministic first.</strong> If the theory engine can compute the answer, it
 *       does, and the model's job is to explain the result rather than to produce it.
 *   <li><strong>Corpus for examples.</strong> A request for real music is a query against
 *       annotated scores. When it returns nothing, that absence is carried forward as a
 *       fact so the tutor says so instead of filling the gap.
 *   <li><strong>Retrieval for explanations.</strong> Published prose, quoted and cited.
 * </ol>
 *
 * <p>Never throws into a turn. Everything here can be unavailable — no index, no corpus,
 * retrieval switched off — and the tutor then teaches from the theory engine as it always
 * could.
 */
@ApplicationScoped
public class KnowledgeRouter {

    private static final Logger LOG = Logger.getLogger(KnowledgeRouter.class);

    @Inject
    IntentClassifier intentClassifier;

    @Inject
    KnowledgeRetriever retriever;

    @Inject
    ConceptExamples conceptExamples;

    @Inject
    AttributionService attributionService;

    @Inject
    SettingsService settingsService;

    @ConfigProperty(name = "music.knowledge.retrieval.top-k", defaultValue = "4")
    int topK;

    @ConfigProperty(name = "music.knowledge.examples.limit", defaultValue = "2")
    int exampleLimit;

    /**
     * @param conceptId the concept being taught, which anchors retrieval when the learner's
     *     own words are vague
     * @param learnerMessage what they actually said, which may be nothing
     */
    public TutorKnowledge gather(String conceptId, String conceptName, String conceptDescription,
                                 String learnerMessage) {
        try {
            return route(conceptId, conceptName, conceptDescription, learnerMessage);
        } catch (RuntimeException e) {
            LOG.warnf("Knowledge routing failed, teaching without it: %s", e.toString());
            return TutorKnowledge.EMPTY;
        }
    }

    /**
     * Everything the application can find about a question somebody asked directly.
     *
     * <p>Unlike a teaching turn, this always searches. A turn retrieves only when the policy
     * decided the learner needed prose; a question typed into a box is the request, so the
     * only reason to come back with nothing is that there is nothing.
     */
    public TutorKnowledge forQuestion(String question) {
        try {
            Set<RetrievalIntent> intents = intentClassifier.classify(question);

            List<TheoryAnswer> computed = new ArrayList<>();
            TheoryQuestion.answer(question).ifPresent(computed::add);

            if (!settingsService.current().knowledgeEnabled) {
                return new TutorKnowledge(intents, computed, List.of(), List.of(), List.of(), false);
            }

            ExampleRequest request = ExampleRequest.from(question);
            List<MusicalExample> examples = request.namesAHarmony()
                    ? conceptExamples.forQuery(request.romanNumeral(), request.cadence(),
                            request.composer(), exampleLimit)
                    : List.of();
            boolean corpusEmpty = request.namesAHarmony() && examples.isEmpty();

            List<RetrievedChunk> retrieved =
                    retriever.retrieve(new RetrievalQuery(question, null, topK)).chunks();

            return new TutorKnowledge(intents, computed, retrieved, examples,
                    creditsFor(retrieved, examples), corpusEmpty);
        } catch (RuntimeException e) {
            LOG.warnf("Could not gather anything for the question: %s", e.toString());
            return TutorKnowledge.EMPTY;
        }
    }

    private TutorKnowledge route(String conceptId, String conceptName, String conceptDescription,
                                 String learnerMessage) {
        Set<RetrievalIntent> intents = intentClassifier.classify(learnerMessage);

        // 1. Anything calculable is calculated. This happens whether or not knowledge is
        //    switched on, because it needs neither an index nor a network.
        List<TheoryAnswer> computed = new ArrayList<>();
        if (intents.contains(RetrievalIntent.DETERMINISTIC_CALCULATION)) {
            TheoryQuestion.answer(learnerMessage).ifPresent(computed::add);
        }

        if (!settingsService.current().knowledgeEnabled) {
            return new TutorKnowledge(intents, computed, List.of(), List.of(), List.of(), false);
        }

        // 2. A request for real music is answered from scores, or not answered.
        List<MusicalExample> examples = List.of();
        boolean corpusEmpty = false;
        if (intents.contains(RetrievalIntent.HARMONIC_EXAMPLE)) {
            ExampleRequest request = ExampleRequest.from(learnerMessage);
            if (request.namesAHarmony()) {
                // They said what they wanted. Answer that, or report that it is not there.
                examples = conceptExamples.forQuery(
                        request.romanNumeral(), request.cadence(), request.composer(), exampleLimit);
            } else if (conceptId != null) {
                examples = conceptExamples.forConcept(conceptId, exampleLimit);
            }
            corpusEmpty = examples.isEmpty();
        }

        // 3. Explanations come from published prose.
        List<RetrievedChunk> retrieved = List.of();
        if (intents.contains(RetrievalIntent.CONCEPT_EXPLANATION)
                || intents.contains(RetrievalIntent.HARMONIC_EXAMPLE)) {
            RetrievalResult result = retriever.retrieve(new RetrievalQuery(
                    queryFor(conceptName, conceptDescription, learnerMessage), conceptId, topK));
            retrieved = result.chunks();
        }

        return new TutorKnowledge(intents, computed, retrieved, examples,
                creditsFor(retrieved, examples), corpusEmpty);
    }

    /**
     * Credits only what was actually used. Listing every ingested source would look like
     * diligence and be the opposite: a citation nobody can check against the answer.
     */
    private List<Attribution> creditsFor(List<RetrievedChunk> retrieved, List<MusicalExample> examples) {
        Set<String> used = new LinkedHashSet<>();
        retrieved.forEach(chunk -> used.add(chunk.sourceId()));
        examples.forEach(example -> used.add(example.sourceId()));
        List<Attribution> credits = new ArrayList<>();
        used.stream().filter(java.util.Objects::nonNull)
                .forEach(id -> attributionService.forSource(id).ifPresent(credits::add));
        return credits;
    }

    /**
     * The concept anchors the query and the learner's words steer it, with their
     * contribution bounded. A long message should not be able to pull retrieval away from
     * the subject being taught.
     */
    private static String queryFor(String conceptName, String conceptDescription, String learnerMessage) {
        StringBuilder query = new StringBuilder();
        if (conceptName != null) {
            query.append(conceptName).append(". ");
        }
        if (conceptDescription != null) {
            query.append(conceptDescription).append(' ');
        }
        if (learnerMessage != null && !learnerMessage.isBlank()) {
            query.append(learnerMessage.length() > 240 ? learnerMessage.substring(0, 240) : learnerMessage);
        }
        return query.toString().trim();
    }
}
