package de.bintintan.itsm.ticket.rest.interfaces;

import de.bintintan.itsm.ticket.application.TicketService;
import de.bintintan.itsm.ticket.application.TicketNotFoundException;
import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketPriority;
import de.bintintan.itsm.ticket.domain.TicketStatus;
import de.bintintan.itsm.ticket.rest.dto.TicketDTO;
import de.bintintan.itsm.ticket.rest.mapper.TicketMapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketResourceTest {

    @Mock
    TicketService ticketService;

    @Mock
    TicketMapper ticketMapper;

    @InjectMocks
    TicketResource ticketResource;

    @Test
    void returnsTicketDtoWhenTicketExists() {
        UUID id = UUID.randomUUID();
        Ticket ticket = new Ticket("Title", "Description", TicketPriority.MEDIUM, Instant.now());
        TicketDTO dto = new TicketDTO(
                ticket.getId(),
                "Title",
                "Description",
                TicketStatus.OPEN,
                TicketPriority.MEDIUM,
                null,
                ticket.getDueAt(),
                0);
        when(ticketService.findById(id)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        Response response = ticketResource.getTicket(id);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(dto, response.getEntity());
        verify(ticketService).findById(id);
        verify(ticketMapper).toDTO(ticket);
    }

    @Test
    void returnsNotFoundWhenTicketDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(ticketService.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketResource.getTicket(id));
        verify(ticketService).findById(id);
        verifyNoInteractions(ticketMapper);
    }
}
