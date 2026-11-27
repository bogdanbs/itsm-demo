package de.bintintan.itsm.ticket.interfaces.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bintintan.itsm.ticket.application.TicketService;
import de.bintintan.itsm.ticket.application.UpdateTicketCommand;
import de.bintintan.itsm.ticket.application.error.TicketNotFoundException;
import de.bintintan.itsm.ticket.application.error.TicketValidationException;
import de.bintintan.itsm.ticket.domain.Page;
import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketStatus;
import de.bintintan.itsm.ticket.domain.TicketVersionConflictException;
import de.bintintan.itsm.ticket.domain.query.TicketQuery;
import de.bintintan.itsm.ticket.interfaces.dto.TicketDTO;
import de.bintintan.itsm.ticket.interfaces.mapper.TicketMapper;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import java.io.StringReader;
import java.net.URI;
import java.util.UUID;

@Path("tickets")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TicketResource {
    private static final String TICKET_TOKEN = "ticket-";
    private static final String VERSION_TOKEN = "-v";

    @Inject
    private TicketService ticketService;
    @Inject
    private TicketMapper ticketMapper;
    @Context
    private Providers providers;

    @POST
    public Response createTicket(@Valid CreateTicketRequest request, @Context UriInfo uriInfo) {
        Ticket ticket =
                ticketService.create(request.title(), request.description(), request.priority(), request.dueAt(),
                        request.assigneeId());
        TicketDTO ticketDTO = ticketMapper.toDTO(ticket);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(ticket.getId())).build();
        return Response.created(location).entity(ticketDTO).build();
    }

    @GET
    public PageResponse<TicketDTO> getTickets(@QueryParam("page") @DefaultValue("0") @Min(0) int page,
                                              @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        Page<Ticket> ticketPage = ticketService.findTickets(null, null, page, size);
        return new PageResponse<>(ticketPage.content().stream().map(ticketMapper::toDTO).toList(), ticketPage.page(),
                ticketPage.size(), ticketPage.totalElements(), ticketPage.totalPages());
    }

    @GET
    @Path("{id}")
    public Response getTicket(@PathParam("id") UUID id) {
        Ticket ticket = ticketService.findById(id).orElseThrow(() -> new TicketNotFoundException(id));

        EntityTag etag = new EntityTag(getTicketVersionETag(ticket));

        return Response.ok(ticketMapper.toDTO(ticket)).tag(etag).build();
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
        Page<Ticket> ticketPage = ticketService.findTickets(new TicketQuery(status, null, assigneeId, null, null, null), null, page, size);
        return new PageResponse<>(ticketPage.content().stream().map(ticketMapper::toDTO).toList(), ticketPage.page(),
                ticketPage.size(), ticketPage.totalElements(), ticketPage.totalPages());
    }

    @PATCH
    @Path("{id}/merge")
    @Consumes("application/merge-patch+json")
    public Response mergeTicket(@PathParam("id") UUID id, @HeaderParam(HttpHeaders.IF_MATCH) EntityTag ifMatch,
                                JsonObject jsonObject)
            throws TicketVersionConflictException, TicketValidationException {
        if (ifMatch == null) {
            return Response.status(Response.Status.PRECONDITION_REQUIRED).build();
        }
        if (ifMatch.isWeak()) {
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }

        Ticket ticket;
        try {
            Ticket currentTicket = ticketService.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
            JsonValue patchedJson =
                    Json.createMergePatch(jsonObject).apply(toJsonValue(ticketMapper.toDTO(currentTicket)));
            TicketDTO dto = toDtoValue(patchedJson);
            UpdateTicketCommand ticketCommand =
                    new UpdateTicketCommand(dto.title(), dto.description(), dto.priority(), dto.assigneeId(),
                            dto.dueAt());
            ticket = ticketService.update(id, extractVersionNumber(ifMatch.getValue(), id), ticketCommand);
        } catch (JsonProcessingException e) {
            throw new TicketValidationException(e.getOriginalMessage());
        }

        EntityTag etag = new EntityTag(getTicketVersionETag(ticket));
        return Response.ok(ticketMapper.toDTO(ticket)).tag(etag).build();
    }

    @PUT
    @Path("{id}/update")
    public Response updateTicket(@PathParam("id") UUID id, @HeaderParam(HttpHeaders.IF_MATCH) EntityTag ifMatch,
                                 @Valid UpdateTicketRequest updateTicketRequest)
            throws TicketVersionConflictException, TicketValidationException {
        if (ifMatch == null) {
            return Response.status(Response.Status.PRECONDITION_REQUIRED).build();
        }
        if (ifMatch.isWeak()) {
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }

        UpdateTicketCommand ticketCommand =
                new UpdateTicketCommand(updateTicketRequest.title(), updateTicketRequest.description(),
                        updateTicketRequest.priority(), updateTicketRequest.assigneeId(), updateTicketRequest.dueAt());
        Ticket ticket = ticketService.update(id, extractVersionNumber(ifMatch.getValue(), id), ticketCommand);
        EntityTag etag = new EntityTag(getTicketVersionETag(ticket));
        return Response.ok(ticketMapper.toDTO(ticket)).tag(etag).build();
    }

    private static int extractVersionNumber(@Nonnull String expectedVersion, UUID ticketId)
            throws TicketValidationException {
        if (!expectedVersion.startsWith(TICKET_TOKEN) || !expectedVersion.contains(VERSION_TOKEN)) {
            throw new TicketValidationException("Invalid ticket version ETag value");
        }
        int versionIndex = expectedVersion.indexOf(VERSION_TOKEN);
        UUID expectedId = UUID.fromString(expectedVersion.substring(TICKET_TOKEN.length(), versionIndex));
        if (expectedId.compareTo(ticketId) != 0) {
            throw new TicketValidationException("Mismatching ticket id found");
        }
        return Integer.parseInt(expectedVersion.substring(versionIndex + VERSION_TOKEN.length()).trim());
    }

    @Nonnull
    public static String getTicketVersionETag(@Nonnull Ticket ticket) {
        return TICKET_TOKEN + ticket.getId() + VERSION_TOKEN + ticket.getVersion();
    }

    private JsonValue toJsonValue(Object representation) throws JsonProcessingException {
        ContextResolver<ObjectMapper> resolver =
                providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE);
        ObjectMapper mapper = resolver.getContext(TicketDTO.class);

        String json = mapper.writeValueAsString(representation);
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readValue();
        }
    }

    private TicketDTO toDtoValue(JsonValue representation) throws JsonProcessingException {
        ContextResolver<ObjectMapper> resolver =
                providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE);
        ObjectMapper mapper = resolver.getContext(TicketDTO.class);
        return mapper.readValue(representation.toString(), TicketDTO.class);
    }
}
