package fr.lapetina.music.api;

import fr.lapetina.music.api.dto.Views;
import fr.lapetina.music.learner.ConceptMastery;
import fr.lapetina.music.learner.EvidenceService;
import fr.lapetina.music.learner.Learner;
import fr.lapetina.music.learner.LearnerService;
import fr.lapetina.music.learner.LearnerSnapshot;
import jakarta.inject.Inject;
import fr.lapetina.music.exercise.AnswerMode;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/learner")
@Produces(MediaType.APPLICATION_JSON)
public class LearnerResource {

    @Inject
    LearnerService learnerService;

    @Inject
    EvidenceService evidenceService;

    /**
     * Wiping the learner model is a development affordance for starting a scenario from
     * nothing, and is refused unless explicitly enabled. It is on in dev and test and off
     * everywhere else.
     */
    @ConfigProperty(name = "music.dev.allow-reset", defaultValue = "false")
    boolean allowReset;

    @GET
    public LearnerSnapshot snapshot() {
        return learnerService.snapshot(learnerService.current());
    }

    /** Wipes everything known about the learner, and leaves a fresh one in place. */
    @DELETE
    public LearnerSnapshot reset() {
        if (!allowReset) {
            throw new ForbiddenException("Resetting the learner model is disabled");
        }
        return learnerService.snapshot(learnerService.reset());
    }

    /**
     * How the learner wants to practise. "auto" hands the choice back to the tutor, which
     * then infers it from what they actually succeed at.
     */
    @PUT
    @Path("/practice-mode/{mode}")
    @Produces(MediaType.APPLICATION_JSON)
    public LearnerSnapshot choosePracticeMode(@PathParam("mode") String mode) {
        AnswerMode chosen = switch (mode.toLowerCase()) {
            case "auto", "any" -> null;
            case "play", "midi", "keyboard" -> AnswerMode.MIDI;
            case "write", "text", "written" -> AnswerMode.TEXT;
            default -> throw new IllegalArgumentException(
                    "Practice mode must be play, write or auto; got " + mode);
        };
        return learnerService.snapshot(learnerService.choosePracticeMode(chosen));
    }

    @GET
    @Path("/concepts")
    public List<ConceptMastery> concepts() {
        return learnerService.snapshot(learnerService.current()).concepts();
    }

    /** The audit trail: why the tutor believes what it believes. */
    @GET
    @Path("/evidence")
    public List<Views.EvidenceView> evidence(@QueryParam("limit") @DefaultValue("50") int limit) {
        Learner learner = learnerService.current();
        return evidenceService.recent(learner, limit).stream().map(Views.EvidenceView::of).toList();
    }

    @GET
    @Path("/evidence/{conceptId}")
    public List<Views.EvidenceView> evidenceFor(@PathParam("conceptId") String conceptId,
                                                @QueryParam("limit") @DefaultValue("50") int limit) {
        Learner learner = learnerService.current();
        return evidenceService.history(learner, conceptId, limit).stream().map(Views.EvidenceView::of).toList();
    }
}
