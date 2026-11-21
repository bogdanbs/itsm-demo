package de.bintintan.itsm.ticket.application;

import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketPriority;
import de.bintintan.itsm.ticket.domain.TicketRepository;
import de.bintintan.itsm.ticket.domain.TicketStatus;
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
        Ticket dbTicket = new TicketService(ticketRepository).create(title, description, TicketPriority.MEDIUM, Instant.now());

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

        TicketNotFoundException exception = assertThrows(
                TicketNotFoundException.class,
                () -> new TicketService(ticketRepository).close(id));

        assertEquals("Ticket not found: " + id, exception.getMessage());
    }
}
