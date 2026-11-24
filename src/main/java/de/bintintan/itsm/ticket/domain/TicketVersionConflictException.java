package de.bintintan.itsm.ticket.domain;

import java.util.UUID;

public class TicketVersionConflictException extends RuntimeException {
    public TicketVersionConflictException(UUID id, int expectedVersion, int currentVersion) {
        super("Version conflict when updating ticket with id " + id + ". Expected version " + expectedVersion + ", " +
                "current version " + currentVersion);
    }
}
