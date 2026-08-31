package fr.lapetina.music.api;

import fr.lapetina.music.api.dto.Views;
import fr.lapetina.music.concept.Concept;
import fr.lapetina.music.concept.ConceptGraph;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/concepts")
@Produces(MediaType.APPLICATION_JSON)
public class ConceptResource {

    @Inject
    ConceptGraph conceptGraph;

    @GET
    public List<Views.ConceptView> all() {
        return conceptGraph.all().stream().map(this::view).toList();
    }

    @GET
    @Path("/{id}")
    public Views.ConceptView get(@PathParam("id") String id) {
        return conceptGraph.find(id).map(this::view)
                .orElseThrow(() -> new NotFoundException("No such concept: " + id));
    }

    private Views.ConceptView view(Concept concept) {
        return new Views.ConceptView(
                concept.id(),
                concept.name(),
                concept.description(),
                concept.category().name(),
                concept.intrinsicDifficulty(),
                concept.prerequisites(),
                conceptGraph.dependentsOf(concept.id()).stream().map(Concept::id).toList());
    }
}
