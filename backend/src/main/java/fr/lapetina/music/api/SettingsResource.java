package fr.lapetina.music.api;

import fr.lapetina.music.llm.TutorAiFactory;
import fr.lapetina.music.settings.Settings;
import fr.lapetina.music.settings.SettingsService;
import fr.lapetina.music.settings.SettingsUpdate;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * The application's own configuration, editable while it runs.
 *
 * <p>Changing the model here takes effect on the next turn: the language model is rebuilt
 * rather than re-read, so there is no restart and nothing to edit outside the application.
 */
@Path("/api/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SettingsResource {

    @Inject
    SettingsService settingsService;

    @Inject
    TutorAiFactory factory;

    @GET
    public Settings get() {
        return settingsService.current();
    }

    @PUT
    public Settings update(SettingsUpdate update) {
        Settings settings = settingsService.update(update);
        factory.invalidate();
        return settings;
    }

    /** Back to the values the application ships with. */
    @POST
    @Path("/reset")
    public Settings reset() {
        Settings settings = settingsService.reset();
        factory.invalidate();
        return settings;
    }

    /** Which models the Ollama instance actually has, so the field can be a list. */
    @GET
    @Path("/models")
    public Map<String, Object> models() {
        Settings settings = settingsService.current();
        try (var client = java.net.http.HttpClient.newHttpClient()) {
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(settings.baseUrl + "/api/tags"))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Map.of("reachable", false, "models", java.util.List.of());
            }
            var parsed = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
            java.util.List<String> names = new java.util.ArrayList<>();
            parsed.path("models").forEach(node -> names.add(node.path("name").asText()));
            return Map.of("reachable", true, "models", names);
        } catch (Exception unreachable) {
            return Map.of("reachable", false, "models", java.util.List.of());
        }
    }
}
