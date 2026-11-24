package de.bintintan.itsm.ticket.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.bintintan.itsm.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateTicketRequest(
         @NotNull @NotBlank String title,
        String description,
         @NotNull TicketPriority priority,
         @NotNull UUID assigneeId,
         @NotNull Instant dueAt
) {
}
