package de.bintintan.itsm.ticket.rest.error;

import de.bintintan.itsm.common.interfaces.rest.error.*;
import de.bintintan.itsm.ticket.application.TicketNotFoundException;
import de.bintintan.itsm.ticket.domain.InvalidStatusTransitionException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketExceptionMapperTest {

    @Mock
    ConstraintViolation<Object> violation;

    @Mock
    Path propertyPath;

    @Mock
    Path.Node propertyNode;

    @Test
    void mapsMissingTicketToNotFound() {
        Response response = new TicketNotFoundExceptionMapper()
                .toResponse(new TicketNotFoundException(java.util.UUID.randomUUID()));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());
        ErrorResponse body = assertInstanceOf(ErrorResponse.class, response.getEntity());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), body.status());
        assertEquals("Ticket not found", body.title());
        assertNotNull(body.timestamp());
    }

    @Test
    void mapsTicketProcessingFailuresToConflict() {
        Response response = new TicketProcessingExceptionMapper()
                .toResponse(new InvalidStatusTransitionException("Invalid ticket state"));

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        ErrorResponse body = assertInstanceOf(ErrorResponse.class, response.getEntity());
        assertEquals(Response.Status.CONFLICT.getStatusCode(), body.status());
        assertEquals("Invalid ticket state", body.message());
        assertNotNull(body.timestamp());
    }

    @Test
    void mapsConstraintViolationsToBadRequestWithFieldDetails() {
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must not be blank");
        when(propertyPath.iterator()).thenReturn(List.of(propertyNode).iterator());
        when(propertyNode.getName()).thenReturn("title");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        Response response = new ConstraintViolationExceptionMapper().toResponse(exception);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ValidationErrorResponse body = assertInstanceOf(ValidationErrorResponse.class, response.getEntity());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), body.status());
        assertEquals(1, body.violations().size());
        assertEquals("title", body.violations().get(0).field());
        assertEquals("must not be blank", body.violations().get(0).message());
    }

    @Test
    void mapsUnexpectedExceptionsToGenericInternalError() {
        Response response = new GenericExceptionMapper()
                .toResponse(new IllegalStateException("sensitive database detail"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        ErrorResponse body = assertInstanceOf(ErrorResponse.class, response.getEntity());
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), body.status());
        assertEquals("An unexpected error occurred.", body.message());
        assertFalse(body.message().contains("sensitive database detail"));
    }

    @Test
    void preservesWebApplicationExceptionResponse() {
        Response original = Response.status(Response.Status.FORBIDDEN)
                .header("X-Test", "preserved")
                .entity("Existing response")
                .build();

        Response response = new GenericExceptionMapper().toResponse(new WebApplicationException(original));

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        assertEquals("preserved", response.getHeaderString("X-Test"));
        assertEquals("Existing response", response.getEntity());
    }
}
