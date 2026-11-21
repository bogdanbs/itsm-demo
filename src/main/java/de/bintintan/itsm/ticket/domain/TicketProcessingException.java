package de.bintintan.itsm.ticket.domain;

public class TicketProcessingException extends IllegalStateException {
    public TicketProcessingException(String message) {
        super(message);
    }
}
