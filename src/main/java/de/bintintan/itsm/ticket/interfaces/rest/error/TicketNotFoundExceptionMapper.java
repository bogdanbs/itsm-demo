package de.bintintan.itsm.ticket.interfaces.rest.error;

import de.bintintan.itsm.common.interfaces.rest.error.ErrorResponse;
import de.bintintan.itsm.ticket.application.error.TicketNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Provider
public class TicketNotFoundExceptionMapper implements ExceptionMapper<TicketNotFoundException> {

    @Override
    public Response toResponse(TicketNotFoundException exception) {
        ErrorResponse body = new ErrorResponse(Response.Status.NOT_FOUND.getStatusCode(), "Ticket not found",
                exception.getLocalizedMessage(), Instant.now().truncatedTo(ChronoUnit.MILLIS));

        return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}
