package fr.lapetina.music.knowledge.router;

import fr.lapetina.music.knowledge.attribution.Attribution;
import fr.lapetina.music.knowledge.harmony.MusicalExample;
import fr.lapetina.music.knowledge.retrieval.RetrievedChunk;
import java.util.List;
import java.util.Set;

/**
 * Everything gathered for one turn, and where each part came from.
 *
 * <p>The three kinds are kept apart all the way to the prompt. Computed facts are certain,
 * quoted prose is somebody else's claim, and corpus examples are facts about a score. A
 * single blob would let the model treat them as equally authoritative, which is precisely
 * how an invented measure number ends up next to a real one.
 */
public record TutorKnowledge(
        Set<RetrievalIntent> intents,
        List<TheoryAnswer> computed,
        List<RetrievedChunk> retrieved,
        List<MusicalExample> examples,
        List<Attribution> sources,
        boolean corpusSearchedAndEmpty) {

    public static final TutorKnowledge EMPTY =
            new TutorKnowledge(Set.of(), List.of(), List.of(), List.of(), List.of(), false);

    public TutorKnowledge {
        intents = Set.copyOf(intents);
        computed = List.copyOf(computed);
        retrieved = List.copyOf(retrieved);
        examples = List.copyOf(examples);
        sources = List.copyOf(sources);
    }

    public boolean isEmpty() {
        return computed.isEmpty() && retrieved.isEmpty() && examples.isEmpty()
                && !corpusSearchedAndEmpty;
    }

    /** The operations performed, for the provenance record. */
    public List<String> theoryOperations() {
        return computed.stream().map(TheoryAnswer::operation).toList();
    }
}
