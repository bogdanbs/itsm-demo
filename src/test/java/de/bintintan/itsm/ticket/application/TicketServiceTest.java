package de.bintintan.itsm.ticket.application;

import de.bintintan.itsm.ticket.application.error.TicketNotFoundException;
import de.bintintan.itsm.ticket.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    TicketRepository ticketRepository;

    @Test
    void createTicket() {
        String title = "Test ticket";
        String description = "Ticket description";
        Ticket ticket = new Ticket(title, description, TicketPriority.MEDIUM, Instant.now());
        when(ticketRepository.save(any())).thenReturn(ticket);
        Ticket dbTicket =
                new TicketService(ticketRepository).create(title, description, TicketPriority.MEDIUM, Instant.now(),
                        null);

        assertEquals(TicketStatus.OPEN, dbTicket.getStatus());
        assertEquals(title, dbTicket.getTitle());
        assertEquals(description, dbTicket.getDescription());
        assertNull(dbTicket.getAssigneeId());
    }

    @Test
    void startsTicketFoundByUuid() {
        UUID id = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        Ticket ticket = new Ticket("Test ticket", null, TicketPriority.MEDIUM, Instant.now());
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));

        new TicketService(ticketRepository).startTicket(id, assigneeId);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(assigneeId, ticket.getAssigneeId());
        verify(ticketRepository).findById(id);
    }

    @Test
    void reportsMissingTicket() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        TicketNotFoundException exception =
                assertThrows(TicketNotFoundException.class, () -> new TicketService(ticketRepository).close(id));

        assertEquals("Ticket not found: " + id, exception.getMessage());
    }

    @Test
    void updatesTicketWhenIfMatchVersionIsCurrent() throws Exception {
        Ticket ticket = new Ticket("Original title", "Original description", TicketPriority.MEDIUM, Instant.now());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        UpdateTicketCommand ticketCommand =
                new UpdateTicketCommand("Updated title", "Updated description", TicketPriority.HIGH, UUID.randomUUID(), null);
        Ticket updated = new TicketService(ticketRepository).update(ticket.getId(), ticket.getVersion(), ticketCommand);

        assertSame(ticket, updated);
        assertEquals("Updated title", updated.getTitle());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(TicketPriority.HIGH, updated.getPriority());
        verify(ticketRepository).findById(ticket.getId());
    }

    @Test
    void rejectsUpdateWhenIfMatchVersionIsStale() {
        Ticket ticket = new Ticket("Original title", null, TicketPriority.MEDIUM, Instant.now());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        UpdateTicketCommand ticketCommand = new UpdateTicketCommand("Updated title", null, null, null, null);

        assertThrows(TicketVersionConflictException.class,
                () -> new TicketService(ticketRepository).update(ticket.getId(), 1, ticketCommand));

        assertEquals("Original title", ticket.getTitle());
        verify(ticketRepository).findById(ticket.getId());
    }
}
