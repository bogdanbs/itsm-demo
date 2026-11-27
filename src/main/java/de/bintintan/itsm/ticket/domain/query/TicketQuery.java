package de.bintintan.itsm.ticket.domain.query;

import de.bintintan.itsm.ticket.domain.TicketPriority;
import de.bintintan.itsm.ticket.domain.TicketStatus;

import java.util.UUID;

public record TicketQuery(TicketStatus status, TicketPriority priority,
                          UUID assigneeId, String titleContains, TimeRange dueAt, TimeRange createdAt) { }

