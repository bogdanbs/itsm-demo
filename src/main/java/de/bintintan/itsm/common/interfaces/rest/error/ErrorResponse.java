package de.bintintan.itsm.common.interfaces.rest.error;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String title,
        String message,
        Instant timestamp
) {
}
