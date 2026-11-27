package de.bintintan.itsm.ticket.infrastructure.persistence;

import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketRepository;
import de.bintintan.itsm.ticket.domain.query.TicketQuery;
import de.bintintan.itsm.ticket.domain.query.TicketSort;
import de.bintintan.itsm.ticket.domain.query.TicketSortField;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.time.Instant;
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
    public long count(@Nullable TicketQuery query) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Ticket> root = countQuery.from(Ticket.class);

        countQuery.select(cb.count(root));

        List<Predicate> predicates = buildPredicates(cb, root, query);
        if (!predicates.isEmpty()) {
            countQuery.where(predicates.toArray(new Predicate[0]));
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    @Override
    public List<Ticket> search(TicketQuery ticketQuery, TicketSort sort, int page, int size) {
        int offset = pageOffset(page, size);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Ticket> query = cb.createQuery(Ticket.class);
        Root<Ticket> root = query.from(Ticket.class);

        // 1. Filters
        List<Predicate> predicates = buildPredicates(cb, root, ticketQuery);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[0]));
        }

        // 2. Sorting
        List<Order> orders = buildOrders(cb, root, sort);
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }

        TypedQuery<Ticket> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(offset).setMaxResults(size);

        return typedQuery.getResultList();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Ticket> root, @Nullable TicketQuery ticketQuery) {
        List<Predicate> predicates = new ArrayList<>();
        if (ticketQuery == null) {
            return predicates;
        }

        // TODO advanced seacrh mode: begins with / contains
        if (ticketQuery.status() != null) {
            predicates.add(cb.equal(root.get("status"), ticketQuery.status()));
        }
        if (ticketQuery.priority() != null) {
            predicates.add(cb.equal(root.get("priority"), ticketQuery.priority()));
        }
        if (ticketQuery.titleContains() != null) {
            predicates.add(cb.like(cb.lower(root.get("title")), "%" + ticketQuery.titleContains().toLowerCase() + "%"));
        }
        if (ticketQuery.createdAt() != null) {
            Path<Instant> dueAt = root.get("createdAt");
            if (ticketQuery.createdAt().from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(dueAt, ticketQuery.createdAt().from()));
            }
            if (ticketQuery.createdAt().to() != null) {
                predicates.add(cb.lessThan(dueAt, ticketQuery.createdAt().to()));
            }
        }
        if (ticketQuery.dueAt() != null) {
            Path<Instant> dueAt = root.get("dueAt");
            if (ticketQuery.dueAt().from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(dueAt, ticketQuery.dueAt().from()));
            }
            if (ticketQuery.dueAt().to() != null) {
                predicates.add(cb.lessThan(dueAt, ticketQuery.dueAt().to()));
            }
        }
        if (ticketQuery.assigneeId() != null) {
            predicates.add(cb.equal(root.get("assigneeId"), ticketQuery.assigneeId()));
        }

        return predicates;
    }

    private List<Order> buildOrders(CriteriaBuilder cb, Root<Ticket> root, @Nullable TicketSort sortBy) {
        List<Order> orders = new ArrayList<>();
        if (sortBy == null) {
            orders.add(cb.desc(root.get("createdAt")));
            orders.add(cb.asc(root.get("id")));
            return orders;
        }

        Path<?> path = root.get(mapSortFieldToModel(sortBy.field()));
        if (sortBy.ascending()) {
            orders.add(cb.asc(path));
        } else {
            orders.add(cb.desc(path));
        }
        orders.add(cb.asc(root.get("id")));
        return orders;
    }

    private String mapSortFieldToModel(TicketSortField field) {
        return switch (field) {
            case TITLE -> "title";
            case DUE_AT -> "dueAt";
            case CREATED_AT -> "createdAt";
            case STATUS -> "status";
            case PRIORITY -> "priority";
            case ASSIGNEE_ID -> "assigneeId";
            case null -> throw new IllegalArgumentException("not supported field found:" + field);
        };
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
}
