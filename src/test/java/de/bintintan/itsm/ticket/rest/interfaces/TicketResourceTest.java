package de.bintintan.itsm.ticket.rest.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bintintan.itsm.common.interfaces.rest.config.JacksonConfig;
import de.bintintan.itsm.ticket.application.TicketService;
import de.bintintan.itsm.ticket.application.UpdateTicketCommand;
import de.bintintan.itsm.ticket.application.error.TicketNotFoundException;
import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketPriority;
import de.bintintan.itsm.ticket.domain.TicketStatus;
import de.bintintan.itsm.ticket.rest.dto.TicketDTO;
import de.bintintan.itsm.ticket.rest.dto.UpdateTicketRequest;
import de.bintintan.itsm.ticket.rest.mapper.TicketMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Providers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketResourceTest {

    @Mock
    TicketService ticketService;

    @Mock
    TicketMapper ticketMapper;

    @Mock
    Providers providers;

    @InjectMocks
    TicketResource ticketResource;

    @Test
    void returnsTicketDtoWhenTicketExists() {
        UUID id = UUID.randomUUID();
        Ticket ticket = new Ticket("Title", "Description", TicketPriority.MEDIUM, Instant.now());
        TicketDTO dto =
                new TicketDTO(ticket.getId(), "Title", "Description", TicketStatus.OPEN, TicketPriority.MEDIUM, null,
                        ticket.getDueAt(), 0);
        when(ticketService.findById(id)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        Response response = ticketResource.getTicket(id);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(dto, response.getEntity());
        assertEquals("\"" + TicketResource.getTicketVersionETag(ticket) + "\"",
                response.getHeaderString(HttpHeaders.ETAG));
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

    @Test
    void returnsUpdatedTicketAndNewEtagWithoutReloadingIt() throws Exception {
        Ticket ticket = new Ticket("Updated title", "Description", TicketPriority.MEDIUM, Instant.now());
        UpdateTicketRequest request = new UpdateTicketRequest("Updated title", null, null, null, null);
        UpdateTicketCommand ticketCommand = new UpdateTicketCommand(request.title(), null, null, null, null);
        TicketDTO dto = new TicketDTO(ticket.getId(), ticket.getTitle(), ticket.getDescription(), TicketStatus.OPEN,
                TicketPriority.MEDIUM, null, ticket.getDueAt(), ticket.getVersion());
        when(ticketService.update(ticket.getId(), ticket.getVersion(), ticketCommand)).thenReturn(ticket);
        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        String expectedEtag = TicketResource.getTicketVersionETag(ticket);
        try (Response response = ticketResource.updateTicket(ticket.getId(), new EntityTag(expectedEtag), request)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals(dto, response.getEntity());
            assertEquals("\"" + expectedEtag + "\"", response.getHeaderString(HttpHeaders.ETAG));
        }
        verify(ticketService).update(ticket.getId(), ticket.getVersion(), ticketCommand);
        verify(ticketService, never()).findById(ticket.getId());
    }

    @Test
    void requiresIfMatchHeaderForUpdate() throws Exception {
        UUID id = UUID.randomUUID();

        try (Response response = ticketResource.updateTicket(id, null,
                new UpdateTicketRequest("Updated title", null, null, null, null))) {
            assertEquals(Response.Status.PRECONDITION_REQUIRED.getStatusCode(), response.getStatus());
            assertNull(response.getEntity());
        }
        verifyNoInteractions(ticketService, ticketMapper);
    }

    @Test
    void rejectsWeakIfMatchHeader() throws Exception {
        UUID id = UUID.randomUUID();
        EntityTag weakTag = new EntityTag("ticket-" + id + "-v0", true);

        try (Response response = ticketResource.updateTicket(id, weakTag,
                new UpdateTicketRequest("Updated title", null, null, null, null))) {
            assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
        }
        verifyNoInteractions(ticketService, ticketMapper);
    }

    @Test
    void mergePatchUpdatesSpecifiedFieldsAndPreservesUnspecifiedFields() throws Exception {
        UUID assigneeId = UUID.randomUUID();
        Ticket ticket = new Ticket("Original title", "Original description", TicketPriority.MEDIUM, null);
        ticket.assignTo(assigneeId);
        UUID id = ticket.getId();

        TicketDTO currentDto =
                new TicketDTO(id, "Original title", "Original description", TicketStatus.OPEN, TicketPriority.MEDIUM,
                        assigneeId, null, ticket.getVersion());
        TicketDTO updatedDto =
                new TicketDTO(id, "Patched title", null, TicketStatus.OPEN, TicketPriority.HIGH, assigneeId, null,
                        ticket.getVersion());
        JsonObject patch = Json.createObjectBuilder()
                .add("title", "Patched title")
                .addNull("description")
                .add("priority", "HIGH")
                .build();
        UpdateTicketCommand expectedCommand =
                new UpdateTicketCommand("Patched title", null, TicketPriority.HIGH, assigneeId, null);
        when(ticketService.findById(id)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDTO(ticket)).thenReturn(currentDto, updatedDto);
        when(providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE))
                .thenReturn(new JacksonConfig());
        when(ticketService.update(id, ticket.getVersion(), expectedCommand)).thenReturn(ticket);

        try (Response response = ticketResource.mergeTicket(
                id, new EntityTag(TicketResource.getTicketVersionETag(ticket)), patch)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals(updatedDto, response.getEntity());
            assertEquals("\"" + TicketResource.getTicketVersionETag(ticket) + "\"",
                    response.getHeaderString(HttpHeaders.ETAG));
        }

        verify(ticketService).findById(id);
        verify(ticketService).update(id, ticket.getVersion(), expectedCommand);
        verify(ticketMapper, times(2)).toDTO(ticket);
    }

    @Test
    void mergePatchRequiresIfMatchHeader() throws Exception {
        UUID id = UUID.randomUUID();
        JsonObject patch = Json.createObjectBuilder().add("title", "Patched title").build();

        try (Response response = ticketResource.mergeTicket(id, null, patch)) {
            assertEquals(Response.Status.PRECONDITION_REQUIRED.getStatusCode(), response.getStatus());
            assertNull(response.getEntity());
        }

        verifyNoInteractions(ticketService, ticketMapper);
    }

    @Test
    void mergePatchRejectsWeakIfMatchHeader() throws Exception {
        UUID id = UUID.randomUUID();
        EntityTag weakTag = new EntityTag("ticket-" + id + "-v0", true);
        JsonObject patch = Json.createObjectBuilder().add("title", "Patched title").build();

        try (Response response = ticketResource.mergeTicket(id, weakTag, patch)) {
            assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
            assertNull(response.getEntity());
        }

        verifyNoInteractions(ticketService, ticketMapper);
    }

    @Test
    void mergePatchReportsNotFoundTicket() {
        UUID id = UUID.randomUUID();
        JsonObject patch = Json.createObjectBuilder().add("title", "Patched title").build();
        when(ticketService.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketResource.mergeTicket(
                id, new EntityTag("ticket-" + id + "-v0"), patch));

        verify(ticketService).findById(id);
        verifyNoInteractions(ticketMapper);
    }
}
