package de.bintintan.itsm.common.interfaces.rest.error;

import java.time.Instant;
import java.util.List;

public record ValidationErrorResponse(
        int status,
        String title,
        Instant timestamp,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {}
}
