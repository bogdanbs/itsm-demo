package de.bintintan.itsm.ticket.interfaces.rest.error;

import de.bintintan.itsm.common.interfaces.rest.error.ErrorResponse;
import de.bintintan.itsm.ticket.application.error.TicketValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Provider
public class TicketValidationExceptionMapper implements ExceptionMapper<TicketValidationException> {

    @Override
    public Response toResponse(TicketValidationException exception) {
        ErrorResponse body =
                new ErrorResponse(Response.Status.CONFLICT.getStatusCode(), "Ticket validation error",
                        exception.getLocalizedMessage(), Instant.now().truncatedTo(ChronoUnit.MILLIS));

        return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}
