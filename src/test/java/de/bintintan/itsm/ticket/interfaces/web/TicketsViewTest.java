package de.bintintan.itsm.ticket.interfaces.web;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TicketsViewTest {

    @Test
    void formatsDueAtInZurichTime() {
        TicketsView ticketsView = new TicketsView();

        assertEquals("29.11.2026 14:07",
                ticketsView.formatDueAt(Instant.parse("2026-11-29T13:07:55.640601Z")));
    }

    @Test
    void returnsNullWhenDueAtIsNull() {
        TicketsView ticketsView = new TicketsView();

        assertNull(ticketsView.formatDueAt(null));
    }
}
