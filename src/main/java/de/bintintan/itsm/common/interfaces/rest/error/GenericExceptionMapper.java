package de.bintintan.itsm.common.interfaces.rest.error;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webEx) {
            return webEx.getResponse();
        }

        if (hasOptimisticLockCause(exception)) {
            ErrorResponse body = new ErrorResponse(
                    Response.Status.PRECONDITION_FAILED.getStatusCode(),
                    "Ticket version conflict",
                    "The ticket has changed; retrieve it and retry.",
                    Instant.now()
            );
            return Response.status(Response.Status.PRECONDITION_FAILED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body)
                    .build();
        }

        LOG.log(Level.SEVERE, "Unhandled exception occurred", exception);

        ErrorResponse body = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred.",
                Instant.now()
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private boolean hasOptimisticLockCause(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException) {
                return true;
            }
        }
        return false;
    }
}
