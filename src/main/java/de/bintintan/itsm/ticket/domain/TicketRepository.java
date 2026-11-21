package de.bintintan.itsm.ticket.domain;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(UUID id);

    Page<Ticket> findTickets(int page, int size);

    Page<Ticket> searchTickets(TicketStatus status, UUID assigneeId, int page, int size);
}
