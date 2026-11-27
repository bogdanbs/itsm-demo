package de.bintintan.itsm.ticket.interfaces.rest.error;

import de.bintintan.itsm.common.interfaces.rest.error.ErrorResponse;
import de.bintintan.itsm.ticket.domain.TicketProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Provider
public class TicketProcessingExceptionMapper implements ExceptionMapper<TicketProcessingException> {

    @Override
    public Response toResponse(TicketProcessingException exception) {
        ErrorResponse body =
                new ErrorResponse(Response.Status.CONFLICT.getStatusCode(), "Ticketing processing error",
                        exception.getLocalizedMessage(), Instant.now().truncatedTo(ChronoUnit.MILLIS));

        return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}
