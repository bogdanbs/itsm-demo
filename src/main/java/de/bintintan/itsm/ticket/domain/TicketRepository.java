package de.bintintan.itsm.ticket.domain;

import de.bintintan.itsm.ticket.domain.query.TicketQuery;
import de.bintintan.itsm.ticket.domain.query.TicketSort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(UUID id);

    List<Ticket> search(TicketQuery query, TicketSort sort, int page, int size);

    long count(TicketQuery query);
}
