package fr.lapetina.music.api;

import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Turns the domain's own complaints into ordinary 400s rather than 500s. */
public class ApiExceptionMappers {

    @ServerExceptionMapper
    public Response illegalArgument(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", String.valueOf(exception.getMessage())))
                .build();
    }

    @ServerExceptionMapper
    public Response illegalState(IllegalStateException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", String.valueOf(exception.getMessage())))
                .build();
    }
}
