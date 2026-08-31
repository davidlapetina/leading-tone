package fr.lapetina.music.api;

import fr.lapetina.music.api.dto.Requests;
import fr.lapetina.music.api.dto.Views;
import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.exercise.Exercise;
import fr.lapetina.music.exercise.ExerciseService;
import fr.lapetina.music.learner.LearnerService;
import fr.lapetina.music.midi.MidiPerformance;
import fr.lapetina.music.tutor.SessionService;
import fr.lapetina.music.tutor.TutorOrchestrator;
import fr.lapetina.music.tutor.TutorSession;
import fr.lapetina.music.tutor.TutorTurn;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.UUID;

@Path("/api/exercises")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExerciseResource {

    @Inject
    ExerciseService exerciseService;

    @Inject
    TutorOrchestrator orchestrator;

    @Inject
    SessionService sessionService;

    @Inject
    LearnerService learnerService;

    @GET
    @Path("/{id}")
    public Views.ExerciseView get(@PathParam("id") UUID id) {
        return Views.ExerciseView.of(exercise(id));
    }

    /** Generates an exercise outside a conversation, which is mostly useful for testing. */
    @POST
    public Views.ExerciseView create(@Valid Requests.ExerciseRequest request) {
        AnswerMode mode = request.answerMode() == null
                ? AnswerMode.TEXT
                : AnswerMode.valueOf(request.answerMode().toUpperCase());
        double difficulty = request.difficulty() == null ? 0.5 : request.difficulty();
        return Views.ExerciseView.of(exerciseService.create(learnerService.current(), null,
                request.conceptId(), difficulty, mode));
    }

    @POST
    @Path("/{id}/answer")
    public TutorTurn answer(@PathParam("id") UUID id, Requests.TextAnswerRequest request) {
        Exercise exercise = exercise(id);
        return orchestrator.answerWithText(exercise.learner, sessionFor(exercise), id, request.answer());
    }

    @POST
    @Path("/{id}/midi")
    public TutorTurn midi(@PathParam("id") UUID id, @Valid Requests.MidiAnswerRequest request) {
        Exercise exercise = exercise(id);
        return orchestrator.answerWithMidi(exercise.learner, sessionFor(exercise), id,
                MidiPerformance.of(request.notes()));
    }

    /**
     * Answering is always a teaching interaction, so it always yields the next turn. An
     * exercise created outside a conversation is attached to the learner's current one
     * rather than answered into a void, which keeps a single response shape on this
     * endpoint instead of two that differ by how the exercise happened to be made.
     */
    private TutorSession sessionFor(Exercise exercise) {
        if (exercise.sessionId != null) {
            Optional<TutorSession> existing = sessionService.find(exercise.sessionId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return sessionService.currentOrNew(exercise.learner);
    }

    private Exercise exercise(UUID id) {
        return exerciseService.find(id)
                .orElseThrow(() -> new NotFoundException("No such exercise: " + id));
    }
}
