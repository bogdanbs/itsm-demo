package de.bintintan.itsm.ticket.interfaces.rest.error;

import de.bintintan.itsm.common.interfaces.rest.error.ErrorResponse;
import de.bintintan.itsm.ticket.domain.TicketVersionConflictException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Provider
public class TicketVersionConflictExceptionMapper implements ExceptionMapper<TicketVersionConflictException> {

    @Override
    public Response toResponse(TicketVersionConflictException exception) {
        ErrorResponse body =
                new ErrorResponse(Response.Status.PRECONDITION_FAILED.getStatusCode(), "Ticket version conflict error - precondition failed",
                        exception.getLocalizedMessage(), Instant.now().truncatedTo(ChronoUnit.MILLIS));

        return Response.status(Response.Status.PRECONDITION_FAILED).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}
