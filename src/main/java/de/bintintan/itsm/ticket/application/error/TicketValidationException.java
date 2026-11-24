package de.bintintan.itsm.ticket.application.error;

public class TicketValidationException extends Exception {
    public TicketValidationException(String message) {
        super(message);
    }
}
