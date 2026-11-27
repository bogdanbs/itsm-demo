package de.bintintan.itsm.ticket.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StartTicketRequest(@NotNull(message = "Assignee id is required") UUID assigneeId) {
}
