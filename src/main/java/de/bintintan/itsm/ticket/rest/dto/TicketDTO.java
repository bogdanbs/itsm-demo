package de.bintintan.itsm.ticket.rest.dto;

import de.bintintan.itsm.ticket.domain.TicketPriority;
import de.bintintan.itsm.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketDTO(UUID id, String title, String description, TicketStatus status, TicketPriority priority,
                        UUID assigneeId, Instant dueAt, int version) {
}
