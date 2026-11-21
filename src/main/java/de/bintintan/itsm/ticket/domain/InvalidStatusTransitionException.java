package de.bintintan.itsm.ticket.domain;

public class InvalidStatusTransitionException extends TicketProcessingException {

    private static final String UNEXPECTED_TRANSITION_STATE = "Unexpected transition state";

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(TicketStatus source, TicketStatus target) {
        super(UNEXPECTED_TRANSITION_STATE + " from " + source.name() + " to " + target.name());
    }
}
