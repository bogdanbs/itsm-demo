package de.bintintan.itsm.ticket.domain;

import java.util.EnumSet;
import java.util.Set;

public enum TicketStatus {
    OPEN, IN_PROGRESS, PENDING_USER, ON_HOLD, RESOLVED, CLOSED;

    public Set<TicketStatus> allowedNextStatuses() {
        return switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS, PENDING_USER, ON_HOLD, CLOSED);
            case IN_PROGRESS -> EnumSet.of(PENDING_USER, ON_HOLD, RESOLVED, CLOSED);
            case PENDING_USER -> EnumSet.of(OPEN, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED);
            case ON_HOLD -> EnumSet.of(IN_PROGRESS, PENDING_USER, CLOSED, OPEN);
            case RESOLVED -> EnumSet.of(IN_PROGRESS, CLOSED); // Reopen or confirm closure
            case CLOSED -> EnumSet.of(OPEN); // Reopen closed ticket
        };
    }

    public boolean canTransitionTo(TicketStatus target) {
        return target != null && allowedNextStatuses().contains(target);
    }
}
