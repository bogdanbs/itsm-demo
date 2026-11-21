package de.bintintan.itsm.ticket.infrastructure.persistence;

import de.bintintan.itsm.ticket.domain.Page;
import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketRepository;
import de.bintintan.itsm.ticket.domain.TicketStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaTicketRepository implements TicketRepository {

    @PersistenceContext
    private EntityManager entityManager;

    protected JpaTicketRepository() {
    }

    JpaTicketRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Ticket save(Ticket ticket) {
        entityManager.persist(ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Ticket.class, id));
    }

    @Override
    public Page<Ticket> findTickets(int page, int size) {
        int offset = pageOffset(page, size);
        List<Ticket> items =
                entityManager.createQuery("SELECT t FROM Ticket t ORDER BY t.createdAt DESC, t.id ASC", Ticket.class)
                        .setFirstResult(offset).setMaxResults(size).getResultList();

        long total = entityManager.createQuery("SELECT COUNT(t) FROM Ticket t", Long.class).getSingleResult();

        int totalPages = (int) Math.ceil((double) total / size);

        return new Page<>(items, page, size, total, totalPages);
    }

    @Override
    public Page<Ticket> searchTickets(TicketStatus status, UUID assigneeId, int page, int size) {
        int offset = pageOffset(page, size);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Ticket> dataQuery = cb.createQuery(Ticket.class);
        Root<Ticket> root = dataQuery.from(Ticket.class);

        dataQuery.select(root).where(buildPredicates(cb, root, status, assigneeId))
                .orderBy(cb.desc(root.get("createdAt")), cb.asc(root.get("id")));

        List<Ticket> tickets =
                entityManager.createQuery(dataQuery).setFirstResult(offset).setMaxResults(size).getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Ticket> countRoot = countQuery.from(Ticket.class);
        countQuery.select(cb.count(countRoot)).where(buildPredicates(cb, countRoot, status, assigneeId));

        long totalElements = entityManager.createQuery(countQuery).getSingleResult();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new Page<>(tickets, page, size, totalElements, totalPages);
    }

    private int pageOffset(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        try {
            return Math.multiplyExact(page, size);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("page and size exceed the supported range", e);
        }
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<Ticket> root, TicketStatus status, UUID assigneeId) {
        List<Predicate> predicates = new ArrayList<>();

        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (assigneeId != null) {
            predicates.add(cb.equal(root.get("assigneeId"), assigneeId));
        }

        return predicates.toArray(new Predicate[0]);
    }
}
