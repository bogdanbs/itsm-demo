package de.bintintan.itsm.ticket.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID ASSIGNEE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void startProgressMovesOpenTicketToInProgress() {
        Ticket ticket = newTicket();

        ticket.assignTo(ASSIGNEE_ID);
        ticket.startProgress(NOW);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"IN_PROGRESS", "PENDING_USER", "ON_HOLD", "RESOLVED", "CLOSED"})
    void startProgressRejectsNonOpenTickets(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        assertThrows(IllegalStateException.class, () -> ticket.startProgress(NOW));
        assertEquals(source, ticket.getStatus());
    }

    @Test
    void startProgressRejectsNullTimeWithoutChangingState() {
        Ticket ticket = newTicket();

        ticket.assignTo(ASSIGNEE_ID);

        assertThrows(NullPointerException.class, () -> ticket.startProgress(null));
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }

    @Test
    void startProgressRejectsUnassignedTicket() {
        Ticket ticket = newTicket();

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ticket.startProgress(Instant.now()));
        assertEquals("Ticket must be assigned before work can start.", e.getMessage());
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "PENDING_USER", "ON_HOLD"})
    void assignsNonTerminalTickets(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        ticket.assignTo(ASSIGNEE_ID);

        assertEquals(ASSIGNEE_ID, ticket.getAssigneeId());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESOLVED", "CLOSED"})
    void rejectsAssignmentToTerminalTickets(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        assertThrows(IllegalStateException.class, () -> ticket.assignTo(ASSIGNEE_ID));
    }

    @Test
    void assignToRejectsNullAssignee() {
        Ticket ticket = newTicket();

        assertThrows(NullPointerException.class, () -> ticket.assignTo(null));
        assertNull(ticket.getAssigneeId());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "ON_HOLD"})
    void requestUserInputMovesAllowedTicketsToPending(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        ticket.requestUserInput(NOW);

        assertEquals(TicketStatus.PENDING_USER, ticket.getStatus());
        assertEquals(NOW, ticket.getPendingUserSince());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"PENDING_USER", "RESOLVED", "CLOSED"})
    void requestUserInputRejectsInvalidTransitions(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        assertThrows(InvalidStatusTransitionException.class, () -> ticket.requestUserInput(NOW));
        assertEquals(source, ticket.getStatus());
    }

    @Test
    void userReplyReturnsUnassignedTicketToOpenAndClearsPendingTime() {
        Ticket ticket = ticketAt(TicketStatus.PENDING_USER);

        ticket.onUserReply(NOW.plusSeconds(1));

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNull(ticket.getPendingUserSince());
    }

    @Test
    void userReplyReturnsAssignedTicketToProgressAndClearsPendingTime() {
        Ticket ticket = newTicket();
        ticket.assignTo(ASSIGNEE_ID);
        ticket.requestUserInput(NOW);

        ticket.onUserReply(NOW.plusSeconds(1));

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertNull(ticket.getPendingUserSince());
        assertEquals(ASSIGNEE_ID, ticket.getAssigneeId());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "ON_HOLD", "RESOLVED", "CLOSED"})
    void userReplyDoesNothingUnlessTicketIsPending(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        ticket.onUserReply(NOW);

        assertEquals(source, ticket.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "PENDING_USER"})
    void putOnHoldMovesAllowedTicketsToHold(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        ticket.putOnHold(NOW);

        assertEquals(TicketStatus.ON_HOLD, ticket.getStatus());
        assertNull(ticket.getPendingUserSince());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"ON_HOLD", "RESOLVED", "CLOSED"})
    void putOnHoldRejectsInvalidTransitions(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        assertThrows(InvalidStatusTransitionException.class, () -> ticket.putOnHold(NOW));
        assertEquals(source, ticket.getStatus());
    }

    @Test
    void resumeFromHoldWithoutAssigneeReturnsTicketOpen() {
        Ticket ticket = ticketAt(TicketStatus.ON_HOLD);

        ticket.resumeFromHold(NOW);

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }

    @Test
    void resumeFromHoldReturnsTicketToProgress() {
        Ticket ticket = ticketAt(TicketStatus.ON_HOLD);

        ticket.assignTo(ASSIGNEE_ID);
        ticket.resumeFromHold(NOW);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "PENDING_USER", "RESOLVED", "CLOSED"})
    void resumeFromHoldRejectsTicketsNotOnHold(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        assertThrows(IllegalStateException.class, () -> ticket.resumeFromHold(NOW));
        assertEquals(source, ticket.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"IN_PROGRESS", "PENDING_USER"})
    void resolveMovesAllowedTicketsToResolvedAndRecordsTime(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        ticket.resolve(NOW);

        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
        assertEquals(NOW, ticket.getResolvedAt());
        assertNull(ticket.getClosedAt());
        assertNull(ticket.getPendingUserSince());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "ON_HOLD", "RESOLVED", "CLOSED"})
    void resolveRejectsInvalidTransitions(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        assertThrows(InvalidStatusTransitionException.class, () -> ticket.resolve(NOW));
        assertEquals(source, ticket.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "PENDING_USER", "ON_HOLD", "RESOLVED"})
    void closeMovesAllowedTicketsToClosedAndRecordsTime(TicketStatus source) {
        Ticket ticket = ticketAt(source);
        Instant resolvedAt = ticket.getResolvedAt();

        ticket.close(NOW);

        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        assertEquals(NOW, ticket.getClosedAt());
        assertEquals(resolvedAt, ticket.getResolvedAt());
        assertNull(ticket.getPendingUserSince());
    }

    @Test
    void closeRejectsAlreadyClosedTicket() {
        Ticket ticket = ticketAt(TicketStatus.CLOSED);
        Instant closedAt = ticket.getClosedAt();

        assertThrows(InvalidStatusTransitionException.class, () -> ticket.close(NOW.plusSeconds(1)));
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        assertEquals(closedAt, ticket.getClosedAt());
    }

    @Test
    void reopenMovesResolvedTicketToProgressAndClearsResolutionTime() {
        Ticket ticket = ticketAt(TicketStatus.RESOLVED);

        ticket.reopen(NOW.plusSeconds(1));

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertNull(ticket.getResolvedAt());
        assertNull(ticket.getClosedAt());
    }

    @Test
    void reopenMovesClosedTicketToOpenAndClearsClosedTime() {
        Ticket ticket = ticketAt(TicketStatus.CLOSED);

        ticket.reopen(NOW.plusSeconds(1));

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNull(ticket.getResolvedAt());
        assertNull(ticket.getClosedAt());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "PENDING_USER", "ON_HOLD"})
    void reopenRejectsNonTerminalTickets(TicketStatus source) {
        Ticket ticket = ticketAt(source);

        assertThrows(IllegalStateException.class, () -> ticket.reopen(NOW));
        assertEquals(source, ticket.getStatus());
    }

    private static Ticket newTicket() {
        return new Ticket("Test ticket", "Ticket description", TicketPriority.MEDIUM, NOW.plusSeconds(3600));
    }

    private static Ticket ticketAt(TicketStatus status) {
        Ticket ticket = newTicket();
        switch (status) {
            case OPEN -> {
            }
            case IN_PROGRESS -> {
                ticket.assignTo(ASSIGNEE_ID);
                ticket.startProgress(NOW);
            }
            case PENDING_USER -> ticket.requestUserInput(NOW);
            case ON_HOLD -> ticket.putOnHold(NOW);
            case RESOLVED -> {
                ticket.assignTo(ASSIGNEE_ID);
                ticket.startProgress(NOW);
                ticket.resolve(NOW.plusSeconds(1));
            }
            case CLOSED -> ticket.close(NOW);
        }
        return ticket;
    }
}
