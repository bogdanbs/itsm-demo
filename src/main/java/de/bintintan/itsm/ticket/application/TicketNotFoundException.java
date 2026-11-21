package de.bintintan.itsm.ticket.application;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(UUID id) {
        super("Ticket not found: " + id);
    }
}
