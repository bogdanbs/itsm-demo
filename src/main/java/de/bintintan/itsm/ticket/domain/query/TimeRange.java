package de.bintintan.itsm.ticket.domain.query;

import jakarta.annotation.Nullable;

import java.time.Instant;

public record TimeRange(@Nullable Instant from, @Nullable Instant to) {
    public TimeRange {
        if ((from == null && to == null) || (from != null && to != null && from.isAfter(to))) {
            throw new IllegalArgumentException("Invalid time range. from: " + from + ", to:" + to);
        }
    }
}
