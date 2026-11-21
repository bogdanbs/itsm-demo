package de.bintintan.itsm.ticket.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TicketPersistenceIntegrationTest {

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
    void prePersistSetsCreatedAtBeforeNotNullValidation() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Ticket ticket = new Ticket("Test ticket", "Created by persistence integration test", TicketPriority.MEDIUM,
                    Instant.now().plusSeconds(3600));
            assertNull(ticket.getCreatedAt());

            entityManager.getTransaction().begin();
            assertDoesNotThrow(() -> {
                entityManager.persist(ticket);
                entityManager.flush();
            });
            assertNotNull(ticket.getCreatedAt());
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }
}
