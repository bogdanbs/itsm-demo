package de.bintintan.itsm.ticket.application;

import de.bintintan.itsm.ticket.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
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

    public Ticket create(String title, String description, TicketPriority priority, Instant dueAt) {
        return ticketRepository.save(new Ticket(title, description, priority, dueAt));
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

    public Page<Ticket> searchTickets(TicketStatus status, UUID assigneeId, @Min(0) int page,
                                      @Min(1) @Max(100) int size) {
        return ticketRepository.searchTickets(status, assigneeId, page, size);
    }

    public Page<Ticket> findTickets(@Min(0) int page, @Min(1) @Max(100) int size) {
        return ticketRepository.findTickets(page, size);
    }
}
