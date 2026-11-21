package de.bintintan.itsm.common.interfaces.rest.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ValidationErrorResponse.FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationErrorResponse.FieldViolation(
                        extractFieldName(violation.getPropertyPath()),
                        violation.getMessage()
                ))
                .toList();

        ValidationErrorResponse body = new ValidationErrorResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Validation Failed",
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                violations
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private String extractFieldName(Path propertyPath) {
        String fieldName = "";
        for (Path.Node node : propertyPath) {
            fieldName = node.getName();
        }
        return fieldName;
    }
}
