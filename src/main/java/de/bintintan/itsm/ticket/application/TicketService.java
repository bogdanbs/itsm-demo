package de.bintintan.itsm.ticket.application;

import de.bintintan.itsm.ticket.application.error.TicketNotFoundException;
import de.bintintan.itsm.ticket.domain.*;
import de.bintintan.itsm.ticket.domain.query.TicketQuery;
import de.bintintan.itsm.ticket.domain.query.TicketSort;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class TicketService {
    private TicketRepository ticketRepository;

    protected TicketService() {

    }

    @Inject
    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket create(String title, String description, TicketPriority priority, Instant dueAt, UUID assigneeId) {
        Ticket ticket = new Ticket(title, description, priority, dueAt);
        if (assigneeId != null) {
            ticket.assignTo(assigneeId);
        }
        return ticketRepository.save(ticket);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<Ticket> findById(UUID id) {
        return ticketRepository.findById(id);
    }

    public void startTicket(UUID id, UUID assigneeId) {
        Ticket ticket = findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        ticket.assignTo(assigneeId);
        ticket.startProgress(Instant.now());
    }

    public void close(UUID id) {
        Ticket ticket = findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        ticket.close(Instant.now());
    }

    public void putOnHold(UUID id) {
        Ticket ticket = findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        ticket.putOnHold(Instant.now());
    }

    @Nonnull
    public Ticket update(@Nonnull UUID id, int expectedVersion, UpdateTicketCommand ticketCommand)
            throws TicketVersionConflictException {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        ticket.verifyVersion(expectedVersion);

        ticket.assignTo(ticketCommand.assigneeId());
        ticket.updateData(ticketCommand.title(), ticketCommand.description(), ticketCommand.priority(),
                ticketCommand.dueAt());
        return ticket;
    }

    public long countTickets(TicketQuery ticketQuery) {
        return ticketRepository.count(ticketQuery);
    }

    public List<Ticket> loadTickets(@Nullable TicketQuery ticketQuery, @Nullable TicketSort ticketSort,
                                    @Min(0) int page, @Min(1) @Max(100) int size) {
        return ticketRepository.search(ticketQuery, ticketSort, page, size);
    }

    public Page<Ticket> findTickets(@Nullable TicketQuery ticketQuery, @Nullable TicketSort ticketSort,
                                    @Min(0) int page, @Min(1) @Max(100) int size) {
        long totalElements = ticketRepository.count(ticketQuery);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        List<Ticket> tickets = ticketRepository.search(ticketQuery, ticketSort, page, size);
        return new Page<>(tickets, page, size, totalElements, totalPages);
    }
}
