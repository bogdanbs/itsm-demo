package de.bintintan.itsm.ticket.infrastructure.persistence;

import de.bintintan.itsm.ticket.domain.Page;
import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.domain.TicketPriority;
import de.bintintan.itsm.ticket.domain.TicketStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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

            Page<Ticket> firstPage = repository.findTickets(0, 2);
            Page<Ticket> secondPage = repository.findTickets(1, 2);
            Page<Ticket> finalPage = repository.findTickets(2, 2);
            List<Ticket> allPages = new ArrayList<>();
            allPages.addAll(firstPage.content());
            allPages.addAll(secondPage.content());
            allPages.addAll(finalPage.content());

            assertEquals(2, firstPage.content().size());
            assertEquals(2, secondPage.content().size());
            assertEquals(1, finalPage.content().size());
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

        assertThrows(IllegalArgumentException.class, () -> repository.findTickets(-1, 10));
    }

    @Test
    void rejectsNonPositiveLimit() {
        JpaTicketRepository repository = new JpaTicketRepository();

        assertThrows(IllegalArgumentException.class, () -> repository.findTickets(0, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.findTickets(0, -1));
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

            Page<Ticket> page = repository.findTickets(1, 2);
            Page<Ticket> orderedTickets = repository.findTickets(0, tickets.size());
            List<Ticket> expectedContent = orderedTickets.content().subList(2, 4);

            assertEquals(expectedContent, page.content());
            assertEquals(1, page.page());
            assertEquals(2, page.size());
            assertEquals(7, page.totalElements());
            assertEquals(4, page.totalPages());
            for (Ticket ticket : page.content()) {
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
            Page<Ticket> page = repository(entityManager).findTickets(0, 20);

            assertEquals(List.of(), page.content());
            assertEquals(0, page.totalElements());
            assertEquals(0, page.totalPages());
        } finally {
            rollbackAndClose(entityManager);
        }
    }

    @Test
    void findTicketsRejectsInvalidPagination() {
        JpaTicketRepository repository = repository(null);

        assertThrows(IllegalArgumentException.class, () -> repository.findTickets(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> repository.findTickets(0, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.findTickets(0, -1));
        assertThrows(IllegalArgumentException.class, () -> repository.findTickets(Integer.MAX_VALUE, 2));
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

            Page<Ticket> unfiltered = repository.searchTickets(null, null, 0, 20);
            Page<Ticket> byStatus = repository.searchTickets(TicketStatus.OPEN, null, 0, 20);
            Page<Ticket> byAssignee = repository.searchTickets(null, ASSIGNEE_A, 0, 20);
            Page<Ticket> firstFilteredPage = repository.searchTickets(TicketStatus.OPEN, ASSIGNEE_A, 0, 2);
            Page<Ticket> secondFilteredPage = repository.searchTickets(TicketStatus.OPEN, ASSIGNEE_A, 1, 2);
            Page<Ticket> noMatches = repository.searchTickets(TicketStatus.CLOSED, ASSIGNEE_B, 0, 20);

            assertEquals(7, unfiltered.totalElements());
            assertEquals(tickets.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()),
                    unfiltered.content().stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));

            assertEquals(4, byStatus.totalElements());
            assertEquals(4, byStatus.content().size());
            assertEquals(tickets.stream().filter(ticket -> ticket.getStatus() == TicketStatus.OPEN).map(Ticket::getId)
                            .collect(java.util.stream.Collectors.toSet()),
                    byStatus.content().stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));
            assertEquals(5, byAssignee.totalElements());
            assertEquals(5, byAssignee.content().size());
            assertEquals(tickets.stream().filter(ticket -> ASSIGNEE_A.equals(ticket.getAssigneeId())).map(Ticket::getId)
                            .collect(java.util.stream.Collectors.toSet()),
                    byAssignee.content().stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));

            List<Ticket> bothPages = new ArrayList<>(firstFilteredPage.content());
            bothPages.addAll(secondFilteredPage.content());
            assertEquals(2, firstFilteredPage.content().size());
            assertEquals(1, secondFilteredPage.content().size());
            assertEquals(0, firstFilteredPage.page());
            assertEquals(1, secondFilteredPage.page());
            assertEquals(2, firstFilteredPage.size());
            assertEquals(2, secondFilteredPage.size());
            assertEquals(3, firstFilteredPage.totalElements());
            assertEquals(2, firstFilteredPage.totalPages());
            assertEquals(3, secondFilteredPage.totalElements());
            assertEquals(tickets.stream().filter(ticket -> ticket.getStatus() == TicketStatus.OPEN && ASSIGNEE_A.equals(
                            ticket.getAssigneeId())).map(Ticket::getId).collect(java.util.stream.Collectors.toSet()),
                    bothPages.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));

            assertEquals(List.of(), noMatches.content());
            assertEquals(0, noMatches.totalElements());
            assertEquals(0, noMatches.totalPages());
        } finally {
            rollbackAndClose(entityManager);
        }
    }

    @Test
    void searchTicketsRejectsInvalidPagination() {
        JpaTicketRepository repository = repository(null);

        assertThrows(IllegalArgumentException.class, () -> repository.searchTickets(null, null, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> repository.searchTickets(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.searchTickets(null, null, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> repository.searchTickets(null, null, Integer.MAX_VALUE, 2));
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
