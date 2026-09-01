package fr.lapetina.music.api;

import fr.lapetina.music.knowledge.retrieval.RetrievedChunk;
import fr.lapetina.music.tutor.AskService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Asking a question directly, outside a lesson.
 *
 * <p>Returns the answer together with everything it was built from: what the theory engine
 * computed, the passages retrieved and who published them, and any real bars found. The
 * material is the point as much as the answer is — it is what lets a reader check a claim
 * instead of taking it.
 */
@Path("/api/ask")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AskResource {

    @Inject
    AskService askService;

    public record AskRequest(
            @NotBlank @Size(max = 2000) String question,
            /** Groups follow-up questions. Omit to start a fresh conversation. */
            String conversationId) {}

    @POST
    public Map<String, Object> ask(@Valid AskRequest request) {
        UUID conversation = parse(request.conversationId());
        AskService.Answer answer = askService.ask(request.question().trim(), conversation);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("question", answer.question());
        response.put("answer", answer.answer());
        response.put("conversationId", (conversation == null ? UUID.randomUUID() : conversation).toString());
        response.put("answeredWithoutAModel", answer.withoutAModel());
        response.put("computed", answer.knowledge().computed().stream().map(computed -> {
            Map<String, Object> fact = new LinkedHashMap<>();
            fact.put("operation", computed.operation());
            fact.put("statement", computed.statement());
            fact.put("answer", computed.answer());
            return fact;
        }).toList());
        response.put("passages", answer.knowledge().retrieved().stream().map(AskResource::passage).toList());
        response.put("examples", answer.knowledge().examples());
        response.put("sources", answer.knowledge().sources());
        response.put("corpusSearchedAndEmpty", answer.knowledge().corpusSearchedAndEmpty());
        return response;
    }

    private static Map<String, Object> passage(RetrievedChunk chunk) {
        Map<String, Object> hit = new LinkedHashMap<>();
        hit.put("chunkId", chunk.chunkId());
        hit.put("citation", chunk.citation());
        hit.put("attribution", chunk.attribution());
        hit.put("license", chunk.licenseId());
        hit.put("url", chunk.url());
        hit.put("excerpt", chunk.body().length() > 600 ? chunk.body().substring(0, 600) + "…" : chunk.body());
        return hit;
    }

    /** A conversation id we did not issue is not an error; it just starts a new conversation. */
    private static UUID parse(String id) {
        try {
            return id == null || id.isBlank() ? null : UUID.fromString(id);
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }
}
