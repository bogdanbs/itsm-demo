package de.bintintan.itsm.ticket.application;

import de.bintintan.itsm.ticket.domain.TicketPriority;

import java.time.Instant;
import java.util.UUID;

public record UpdateTicketCommand(String title, String description, TicketPriority priority, UUID assigneeId,
                                  Instant dueAt) {
}
