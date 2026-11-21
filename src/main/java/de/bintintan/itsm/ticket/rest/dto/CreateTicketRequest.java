package de.bintintan.itsm.ticket.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.bintintan.itsm.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateTicketRequest(@NotBlank(message = "Title is required") @Size(min = 3, max = 100) String title,
                                  String description,
                                  @NotNull TicketPriority priority,
                                  @NotNull Instant dueAt) {
}
