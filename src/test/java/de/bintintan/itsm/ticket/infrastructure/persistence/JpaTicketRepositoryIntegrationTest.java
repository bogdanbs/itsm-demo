package de.bintintan.itsm.ticket.infrastructure.persistence;

import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketPriority;
import de.bintintan.itsm.ticket.domain.TicketStatus;
import de.bintintan.itsm.ticket.domain.query.TicketQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaTicketRepositoryIntegrationTest {

    private static final UUID ASSIGNEE_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ASSIGNEE_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant DUE_AT = Instant.parse("2026-11-21T12:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void setUpPersistence() {
        entityManagerFactory = Persistence.createEntityManagerFactory("ticket-test");
    }

    @AfterAll
    static void closePersistence() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void returnsTicketsInPagesOrderedByCreationTime() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            JpaTicketRepository repository = new JpaTicketRepository(entityManager);

            Set<UUID> expectedIds = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                Ticket ticket = new Ticket("Ticket " + i, "Pagination test", TicketPriority.MEDIUM,
                        Instant.now().plusSeconds(3600));
                expectedIds.add(ticket.getId());
                repository.save(ticket);
                entityManager.flush();
            }
            entityManager.clear();

            List<Ticket> firstPage = repository.search(null, null, 0, 2);
            List<Ticket> secondPage = repository.search(null, null, 1, 2);
            List<Ticket> finalPage = repository.search(null, null, 2, 2);
            List<Ticket> allPages = new ArrayList<>();
            allPages.addAll(firstPage);
            allPages.addAll(secondPage);
            allPages.addAll(finalPage);

            assertEquals(2, firstPage.size());
            assertEquals(2, secondPage.size());
            assertEquals(1, finalPage.size());
            assertEquals(expectedIds,
                    allPages.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));
            assertEquals(entityManager.createQuery("SELECT t.id FROM Ticket t ORDER BY t.createdAt DESC, t.id ASC",
                    UUID.class).getResultList(), allPages.stream().map(Ticket::getId).toList());
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Test
    void rejectsNegativeOffset() {
        JpaTicketRepository repository = new JpaTicketRepository();

        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, -1, 10));
    }

    @Test
    void rejectsNonPositiveLimit() {
        JpaTicketRepository repository = new JpaTicketRepository();

        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, 0, -1));
    }

    @Test
    void detectsConcurrentTicketUpdatesUsingVersion() {
        UUID ticketId;
        EntityManager setupManager = entityManagerFactory.createEntityManager();
        try {
            setupManager.getTransaction().begin();
            Ticket ticket = new Ticket("Optimistic locking", "Version test", TicketPriority.MEDIUM, DUE_AT);
            setupManager.persist(ticket);
            setupManager.getTransaction().commit();
            ticketId = ticket.getId();
        } finally {
            if (setupManager.getTransaction().isActive()) {
                setupManager.getTransaction().rollback();
            }
            setupManager.close();
        }

        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager secondManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            secondManager.getTransaction().begin();
            Ticket firstCopy = firstManager.find(Ticket.class, ticketId);
            Ticket secondCopy = secondManager.find(Ticket.class, ticketId);

            firstCopy.updateData("First update", null, null, null);
            secondCopy.updateData("Second update", null, null, null);
            firstManager.getTransaction().commit();

            assertThrows(OptimisticLockException.class, secondManager::flush);
            secondManager.getTransaction().rollback();
        } finally {
            if (firstManager.getTransaction().isActive()) {
                firstManager.getTransaction().rollback();
            }
            if (secondManager.getTransaction().isActive()) {
                secondManager.getTransaction().rollback();
            }
            firstManager.close();
            secondManager.close();

            EntityManager cleanupManager = entityManagerFactory.createEntityManager();
            try {
                cleanupManager.getTransaction().begin();
                Ticket persistedTicket = cleanupManager.find(Ticket.class, ticketId);
                if (persistedTicket != null) {
                    cleanupManager.remove(persistedTicket);
                }
                cleanupManager.getTransaction().commit();
            } finally {
                if (cleanupManager.getTransaction().isActive()) {
                    cleanupManager.getTransaction().rollback();
                }
                cleanupManager.close();
            }
        }
    }

    @Test
    void findTicketsReturnsMappedPageAndUnfilteredTotals() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            JpaTicketRepository repository = repository(entityManager);
            List<Ticket> tickets = seedSearchTickets(repository);
            entityManager.flush();
            entityManager.clear();

            List<Ticket> page = repository.search(null, null, 1, 2);
            long totalPageElements = repository.count(null);
            List<Ticket> orderedTickets = repository.search(null, null, 0, tickets.size());
            List<Ticket> expectedContent = orderedTickets.subList(2, 4);
            int totalPages = (int) Math.ceil((double) totalPageElements / 2);

            assertEquals(expectedContent, page);
            assertEquals(2, page.size());
            assertEquals(7, totalPageElements);
            assertEquals(4, totalPages);
            for (Ticket ticket : page) {
                Ticket source = tickets.stream().filter(pTicket -> pTicket.getId().equals(ticket.getId())).findFirst()
                        .orElseThrow();
                assertMatches(source, ticket);
            }
        } finally {
            rollbackAndClose(entityManager);
        }
    }

    @Test
    void findTicketsReturnsEmptyPageAndZeroPagesForEmptyDatabase() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            List<Ticket> page = repository(entityManager).search(null, null, 0, 20);
            long totalElements = repository(entityManager).count(null);
            int totalPages = (int) Math.ceil((double) totalElements / 20);

            assertEquals(List.of(), page);
            assertEquals(0, totalElements);
            assertEquals(0, totalPages);
        } finally {
            rollbackAndClose(entityManager);
        }
    }

    @Test
    void findTicketsRejectsInvalidPagination() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        JpaTicketRepository repository = repository(entityManager);

        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, Integer.MAX_VALUE, 2));
    }

    @Test
    void searchTicketsAppliesEachOptionalFilterAndPaginatesResults() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            JpaTicketRepository repository = repository(entityManager);
            List<Ticket> tickets = seedSearchTickets(repository);
            entityManager.flush();
            entityManager.clear();

            List<Ticket> unfiltered = repository.search(null, null, 0, 20);
            long totalUnfilteredElements = repository.count(null);
            TicketQuery ticketQuery = new TicketQuery(TicketStatus.OPEN, null, null, null, null, null);
            List<Ticket> byStatus = repository.search(ticketQuery, null, 0, 20);
            long totalByStatusElements = repository.count(ticketQuery);
            ticketQuery = new TicketQuery(null, null, ASSIGNEE_A, null, null, null);
            List<Ticket> byAssignee = repository.search(ticketQuery, null, 0, 20);
            long totalByAssigneeElements = repository.count(ticketQuery);
            ticketQuery = new TicketQuery(TicketStatus.OPEN, null, ASSIGNEE_A, null, null, null);
            List<Ticket> firstFilteredPage = repository.search(ticketQuery, null, 0, 2);
            long totalFirstFilteredPageElements = repository.count(ticketQuery);
            ticketQuery = new TicketQuery(TicketStatus.OPEN, null, ASSIGNEE_A, null, null, null);
            List<Ticket> secondFilteredPage = repository.search(ticketQuery, null, 1, 2);
            long totalSecondFilteredPageElements = repository.count(ticketQuery);
            ticketQuery = new TicketQuery(TicketStatus.CLOSED, null, ASSIGNEE_B, null, null, null);
            List<Ticket> noMatches = repository.search(ticketQuery, null, 0, 20);
            long totalNoMatchesElements = repository.count(ticketQuery);

            assertEquals(7, totalUnfilteredElements);
            assertEquals(tickets.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()),
                    unfiltered.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));

            assertEquals(4, totalByStatusElements);
            assertEquals(4, byStatus.size());
            assertEquals(tickets.stream().filter(ticket -> ticket.getStatus() == TicketStatus.OPEN).map(Ticket::getId)
                            .collect(java.util.stream.Collectors.toSet()),
                    byStatus.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));
            assertEquals(5, totalByAssigneeElements);
            assertEquals(5, byAssignee.size());
            assertEquals(tickets.stream().filter(ticket -> ASSIGNEE_A.equals(ticket.getAssigneeId())).map(Ticket::getId)
                            .collect(java.util.stream.Collectors.toSet()),
                    byAssignee.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));

            List<Ticket> bothPages = new ArrayList<>(firstFilteredPage);
            bothPages.addAll(secondFilteredPage);

            int totalFirstFilteredPages = (int) Math.ceil((double) totalFirstFilteredPageElements / 2);
            int totalNoMatchPages = (int) Math.ceil((double) totalNoMatchesElements / 20);

            assertEquals(2, firstFilteredPage.size());
            assertEquals(1, secondFilteredPage.size());
            assertEquals(3, totalFirstFilteredPageElements);
            assertEquals(2, totalFirstFilteredPages);
            assertEquals(3, totalSecondFilteredPageElements);
            assertEquals(tickets.stream().filter(ticket -> ticket.getStatus() == TicketStatus.OPEN && ASSIGNEE_A.equals(
                            ticket.getAssigneeId())).map(Ticket::getId).collect(java.util.stream.Collectors.toSet()),
                    bothPages.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));

            assertEquals(List.of(), noMatches);
            assertEquals(0, totalNoMatchesElements);
            assertEquals(0, totalNoMatchPages);
        } finally {
            rollbackAndClose(entityManager);
        }
    }

    @Test
    void searchTicketsRejectsInvalidPagination() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        JpaTicketRepository repository = repository(entityManager);

        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, Integer.MAX_VALUE, 2));
    }

    private static JpaTicketRepository repository(EntityManager entityManager) {
        return new JpaTicketRepository(entityManager);
    }

    private static List<Ticket> seedSearchTickets(JpaTicketRepository repository) {
        List<Ticket> tickets = new ArrayList<>();
        tickets.add(createTicket(repository, "Open A 1", TicketStatus.OPEN, ASSIGNEE_A));
        tickets.add(createTicket(repository, "Open A 2", TicketStatus.OPEN, ASSIGNEE_A));
        tickets.add(createTicket(repository, "Open A 3", TicketStatus.OPEN, ASSIGNEE_A));
        tickets.add(createTicket(repository, "Open B", TicketStatus.OPEN, ASSIGNEE_B));
        tickets.add(createTicket(repository, "In progress A", TicketStatus.IN_PROGRESS, ASSIGNEE_A));
        tickets.add(createTicket(repository, "Pending unassigned", TicketStatus.PENDING_USER, null));
        tickets.add(createTicket(repository, "Closed A", TicketStatus.CLOSED, ASSIGNEE_A));
        return tickets;
    }

    private static Ticket createTicket(JpaTicketRepository repository, String title, TicketStatus status,
                                       UUID assigneeId) {
        Ticket ticket = new Ticket(title, "Repository search test", TicketPriority.MEDIUM, DUE_AT);
        if (assigneeId != null) {
            ticket.assignTo(assigneeId);
        }
        switch (status) {
            case OPEN -> {
            }
            case IN_PROGRESS -> ticket.startProgress(NOW);
            case PENDING_USER -> ticket.requestUserInput(NOW);
            case ON_HOLD -> ticket.putOnHold(NOW);
            case RESOLVED -> {
                if (ticket.getAssigneeId() == null) {
                    ticket.assignTo(ASSIGNEE_A);
                }
                ticket.startProgress(NOW);
                ticket.resolve(NOW.plusSeconds(1));
            }
            case CLOSED -> ticket.close(NOW);
        }
        repository.save(ticket);
        return ticket;
    }

    private static void rollbackAndClose(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    private static void assertMatches(Ticket ticket, Ticket dto) {
        assertEquals(ticket.getId(), dto.getId());
        assertEquals(ticket.getTitle(), dto.getTitle());
        assertEquals(ticket.getDescription(), dto.getDescription());
        assertEquals(ticket.getPriority(), dto.getPriority());
        assertEquals(ticket.getAssigneeId(), dto.getAssigneeId());
        assertEquals(ticket.getDueAt(), dto.getDueAt());
    }
}
