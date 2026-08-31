package fr.lapetina.music.concept;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The prerequisite graph, loaded once from {@code concepts.json}.
 *
 * <p>It is the source of truth for what depends on what. Nothing here knows about
 * lessons, chapters or an order of study.
 */
@ApplicationScoped
public class ConceptGraph {

    private static final String RESOURCE = "concepts.json";

    @Inject
    ObjectMapper objectMapper;

    public ConceptGraph() {
    }

    /** Builds a graph outside CDI, for tests and for tooling. */
    public ConceptGraph(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        load();
    }

    private Map<String, Concept> concepts = Map.of();
    private Map<String, List<String>> dependents = Map.of();
    private List<String> topologicalOrder = List.of();

    @PostConstruct
    void init() {
        load();
    }

    /** Exposed so tests can build a graph without booting the application. */
    public void load() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
            }
            List<Concept> loaded = objectMapper.readValue(stream, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Concept.class));
            index(loaded);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + RESOURCE, e);
        }
    }

    void index(List<Concept> loaded) {
        Map<String, Concept> byId = new LinkedHashMap<>();
        for (Concept concept : loaded) {
            if (byId.put(concept.id(), concept) != null) {
                throw new IllegalStateException("Duplicate concept id: " + concept.id());
            }
        }
        for (Concept concept : loaded) {
            for (String prerequisite : concept.prerequisites()) {
                if (!byId.containsKey(prerequisite)) {
                    throw new IllegalStateException(
                            concept.id() + " requires unknown concept " + prerequisite);
                }
            }
        }
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        for (Concept concept : loaded) {
            reverse.computeIfAbsent(concept.id(), key -> new ArrayList<>());
            for (String prerequisite : concept.prerequisites()) {
                reverse.computeIfAbsent(prerequisite, key -> new ArrayList<>()).add(concept.id());
            }
        }
        this.concepts = Map.copyOf(byId);
        this.dependents = Map.copyOf(reverse);
        this.topologicalOrder = sortTopologically(byId);
    }

    private static List<String> sortTopologically(Map<String, Concept> byId) {
        Map<String, Integer> pending = new HashMap<>();
        for (Concept concept : byId.values()) {
            pending.put(concept.id(), concept.prerequisites().size());
        }
        Deque<String> ready = new ArrayDeque<>();
        byId.values().stream()
                .filter(concept -> concept.prerequisites().isEmpty())
                .forEach(concept -> ready.add(concept.id()));

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String current = ready.poll();
            order.add(current);
            for (Concept concept : byId.values()) {
                if (concept.prerequisites().contains(current)) {
                    int remaining = pending.merge(concept.id(), -1, Integer::sum);
                    if (remaining == 0) {
                        ready.add(concept.id());
                    }
                }
            }
        }
        if (order.size() != byId.size()) {
            throw new IllegalStateException("The concept graph contains a prerequisite cycle");
        }
        return List.copyOf(order);
    }

    public List<Concept> all() {
        return topologicalOrder.stream().map(concepts::get).toList();
    }

    public Optional<Concept> find(String id) {
        return Optional.ofNullable(concepts.get(id));
    }

    public Concept require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown concept: " + id));
    }

    public boolean contains(String id) {
        return concepts.containsKey(id);
    }

    /** Concepts that list {@code id} as a prerequisite. */
    public List<Concept> dependentsOf(String id) {
        return dependents.getOrDefault(id, List.of()).stream().map(concepts::get).toList();
    }

    public List<Concept> prerequisitesOf(String id) {
        return require(id).prerequisites().stream().map(concepts::get).toList();
    }

    /** Every prerequisite, transitively, nearest first. */
    public List<Concept> allPrerequisitesOf(String id) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(require(id).prerequisites());
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (seen.add(current)) {
                queue.addAll(require(current).prerequisites());
            }
        }
        return seen.stream().map(concepts::get).toList();
    }

    /**
     * Concepts whose prerequisites are all satisfied according to {@code isKnown},
     * excluding the ones already known. This is the frontier the tutor may teach from.
     */
    public List<Concept> frontier(Predicate<String> isKnown) {
        List<Concept> frontier = new ArrayList<>();
        for (String id : topologicalOrder) {
            Concept concept = concepts.get(id);
            if (isKnown.test(id)) {
                continue;
            }
            if (concept.prerequisites().stream().allMatch(isKnown)) {
                frontier.add(concept);
            }
        }
        return List.copyOf(frontier);
    }

    /** Prerequisites of {@code id} that are not yet known, nearest first. */
    public List<Concept> missingPrerequisites(String id, Predicate<String> isKnown) {
        List<Concept> missing = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Concept prerequisite : allPrerequisitesOf(id)) {
            if (!isKnown.test(prerequisite.id()) && seen.add(prerequisite.id())) {
                missing.add(prerequisite);
            }
        }
        return List.copyOf(missing);
    }

    public List<String> topologicalOrder() {
        return topologicalOrder;
    }

    public int size() {
        return concepts.size();
    }
}
