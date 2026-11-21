package de.bintintan.itsm.ticket.rest.interfaces;

import de.bintintan.itsm.ticket.application.TicketNotFoundException;
import de.bintintan.itsm.ticket.application.TicketService;
import de.bintintan.itsm.ticket.domain.Page;
import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketStatus;
import de.bintintan.itsm.ticket.rest.dto.CreateTicketRequest;
import de.bintintan.itsm.ticket.rest.dto.PageResponse;
import de.bintintan.itsm.ticket.rest.dto.StartTicketRequest;
import de.bintintan.itsm.ticket.rest.dto.TicketDTO;
import de.bintintan.itsm.ticket.rest.mapper.TicketMapper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.UUID;

@Path("tickets")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TicketResource {
    @Inject
    private TicketService ticketService;
    @Inject
    private TicketMapper ticketMapper;

    @POST
    public Response createTicket(@Valid CreateTicketRequest request, @Context UriInfo uriInfo) {
        Ticket ticket =
                ticketService.create(request.title(), request.description(), request.priority(), request.dueAt());
        TicketDTO ticketDTO = ticketMapper.toDTO(ticket);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(ticket.getId())).build();
        return Response.created(location).entity(ticketDTO).build();
    }

    @GET
    public PageResponse<TicketDTO> getTickets(@QueryParam("page") @DefaultValue("0") @Min(0) int page,
                                              @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {

        Page<Ticket> ticketPage = ticketService.findTickets(page, size);
        return new PageResponse<>(ticketPage.content().stream().map(ticketMapper::toDTO).toList(), ticketPage.page(),
                ticketPage.size(), ticketPage.totalElements(), ticketPage.totalPages());
    }

    @GET
    @Path("{id}")
    public Response getTicket(@PathParam("id") UUID id) {
        return ticketService.findById(id).map(ticketMapper::toDTO).map(dto -> Response.ok(dto).build())
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @POST
    @Path("{id}/start")
    public Response startTicket(@PathParam("id") UUID id, @Valid StartTicketRequest request) {
        ticketService.startTicket(id, request.assigneeId());
        return Response.ok().build();
    }

    @POST
    @Path("{id}/hold")
    public Response holdTicket(@PathParam("id") UUID id) {
        ticketService.putOnHold(id);
        return Response.ok().build();
    }

    @GET
    @Path("search")
    public PageResponse<TicketDTO> searchTickets(@QueryParam("status") TicketStatus status,
                                                 @QueryParam("assigneeId") UUID assigneeId,
                                                 @QueryParam("page") @DefaultValue("0") @Min(0) int page,
                                                 @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        Page<Ticket> ticketPage = ticketService.searchTickets(status, assigneeId, page, size);
        return new PageResponse<>(ticketPage.content().stream().map(ticketMapper::toDTO).toList(), ticketPage.page(),
                ticketPage.size(), ticketPage.totalElements(), ticketPage.totalPages());
    }
}
