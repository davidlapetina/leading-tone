package fr.lapetina.music.api;

import fr.lapetina.music.api.dto.Requests;
import fr.lapetina.music.api.dto.Views;
import fr.lapetina.music.concept.ConceptGraph;
import fr.lapetina.music.learner.Learner;
import fr.lapetina.music.learner.LearnerService;
import fr.lapetina.music.llm.TutorModel;
import fr.lapetina.music.tutor.TeachingDecision;
import fr.lapetina.music.tutor.TutorOrchestrator;
import fr.lapetina.music.tutor.TutorSession;
import fr.lapetina.music.tutor.TutorTurn;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/session")
@Produces(MediaType.APPLICATION_JSON)
public class TutorResource {

    @Inject
    LearnerService learnerService;

    @Inject
    TutorOrchestrator orchestrator;

    @Inject
    fr.lapetina.music.tutor.SessionService sessionService;

    @Inject
    TutorModel tutorModel;

    @Inject
    fr.lapetina.music.settings.SettingsService settingsService;

    @Inject
    ConceptGraph conceptGraph;

    /** Starts a session and returns the tutor's opening turn. */
    @POST
    public TutorTurn start() {
        Learner learner = learnerService.current();
        TutorSession session = sessionService.start(learner);
        return orchestrator.open(learner, session);
    }

    @GET
    @Path("/{id}")
    public Views.SessionView get(@PathParam("id") UUID id) {
        TutorSession session = session(id);
        return Views.SessionView.of(session, sessionService.interactions(session));
    }

    @POST
    @Path("/{id}/message")
    @Consumes(MediaType.APPLICATION_JSON)
    public TutorTurn message(@PathParam("id") UUID id, @Valid Requests.MessageRequest request) {
        TutorSession session = session(id);
        return orchestrator.message(session.learner, session, request.message());
    }

    /**
     * The same turn, delivered as server-sent events so the interface can show the
     * decision before the prose arrives. V1 streams the composed turn in stages rather
     * than model tokens.
     *
     * <p>Both annotations are load-bearing, and for different reasons. {@code @Blocking}
     * moves the method body — the session lookup — off the event loop. It does nothing
     * for the emitter, which runs later, when something subscribes; that needs
     * {@code runSubscriptionOn}. Getting only one of them produces a stream that fails
     * the moment it touches the database.
     */
    @GET
    @Path("/{id}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Blocking
    public Multi<Map<String, Object>> stream(@PathParam("id") UUID id, @QueryParam("message") String message) {
        TutorSession session = session(id);
        return Multi.createFrom().<Map<String, Object>>emitter(emitter -> {
            try {
                TeachingDecision decision = orchestrator.preview(session.learner);
                emitter.emit(Map.of(
                        "type", "decision",
                        "action", decision.action().name(),
                        "concept", decision.conceptId(),
                        "rationale", decision.rationale()));
                TutorTurn turn = message == null || message.isBlank()
                        ? orchestrator.open(session.learner, session)
                        : orchestrator.message(session.learner, session, message);
                emitter.emit(Map.of("type", "turn", "turn", turn));
                emitter.emit(Map.of("type", "done"));
                emitter.complete();
            } catch (RuntimeException failure) {
                emitter.fail(failure);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    /** What the policy would choose right now, without teaching anything. */
    @GET
    @Path("/next-action")
    public TeachingDecision nextAction() {
        return orchestrator.preview(learnerService.current());
    }

    @GET
    @Path("/status")
    public Views.TutorStatusView status() {
        var settings = settingsService.current();
        return new Views.TutorStatusView(
                tutorModel.describe(),
                tutorModel.isAvailable(),
                settings.model,
                settings.toolsEnabled,
                conceptGraph.size());
    }

    private TutorSession session(UUID id) {
        return sessionService.find(id)
                .orElseThrow(() -> new NotFoundException("No such session: " + id));
    }
}
