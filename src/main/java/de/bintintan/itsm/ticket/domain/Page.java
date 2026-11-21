package de.bintintan.itsm.ticket.domain;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
