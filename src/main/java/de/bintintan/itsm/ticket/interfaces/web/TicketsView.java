package de.bintintan.itsm.ticket.interfaces.web;

import de.bintintan.itsm.ticket.application.TicketService;
import de.bintintan.itsm.ticket.domain.TicketPriority;
import de.bintintan.itsm.ticket.domain.TicketStatus;
import de.bintintan.itsm.ticket.domain.query.TicketQuery;
import de.bintintan.itsm.ticket.domain.query.TicketSort;
import de.bintintan.itsm.ticket.domain.query.TicketSortField;
import de.bintintan.itsm.ticket.domain.query.TimeRange;
import de.bintintan.itsm.ticket.interfaces.dto.TicketDTO;
import de.bintintan.itsm.ticket.interfaces.mapper.TicketMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Named("ticketsView")
@ViewScoped
public class TicketsView implements Serializable {

    public static final ZoneId EU_CH_ZONE_ID = ZoneId.of("Europe/Zurich");

    private static final DateTimeFormatter DUE_AT_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(EU_CH_ZONE_ID);

    @Inject
    private TicketService ticketService;

    private LazyDataModel<TicketDTO> lazyModel;

    @Inject
    private TicketMapper ticketMapper;

    private final Map<String, TicketSortField> columnMapper =
            Map.of("title", TicketSortField.TITLE, "status", TicketSortField.STATUS, "assigneeId",
                    TicketSortField.ASSIGNEE_ID, "dueAt", TicketSortField.DUE_AT, "createdAt",
                    TicketSortField.CREATED_AT, "priority", TicketSortField.PRIORITY);

    @PostConstruct
    void init() {
        lazyModel = new LazyDataModel<>() {

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                TicketQuery ticketQuery = createQueryFromFilter(filterBy);
                return Math.toIntExact(ticketService.countTickets(ticketQuery));
            }

            private TicketQuery createQueryFromFilter(Map<String, FilterMeta> filterBy) {
                TicketStatus status = null;
                TicketPriority priority = null;
                UUID assigneeId = null;
                String titleContent = null;
                TimeRange dueAt = null;
                TimeRange createdAt = null;

                for (FilterMeta meta : filterBy.values()) {
                    Object filterValue = meta.getFilterValue();
                    if (filterValue == null || filterValue.toString().isBlank()) {
                        continue;
                    }
                    String field = meta.getField();
                    String strFilterValue = filterValue.toString();
                    // MatchMode matchMode = meta.getMatchMode(); -> TODO to be implemented at a later point
                    switch (columnMapper.get(field)) {
                        case STATUS -> status = TicketStatus.valueOf(strFilterValue.toUpperCase());
                        case CREATED_AT -> createdAt = getDateRange(meta);
                        case PRIORITY -> priority = TicketPriority.valueOf(strFilterValue.toUpperCase());
                        case ASSIGNEE_ID -> assigneeId = UUID.fromString(strFilterValue);
                        case DUE_AT -> dueAt = getDateRange(meta);
                        case TITLE -> titleContent = getEscapedValue(strFilterValue);
                    }
                }
                return new TicketQuery(status, priority, assigneeId, titleContent, dueAt, createdAt);
            }

            @Override
            public List<TicketDTO> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                                        Map<String, FilterMeta> filterBy) {
                TicketQuery ticketQuery = createQueryFromFilter(filterBy);
                TicketSort ticketSort = createSortFromMeta(sortBy);
                int page = ((pageSize > 0) ? (first / pageSize) : 0);
                return ticketService.loadTickets(ticketQuery, ticketSort, page, pageSize).stream()
                        .map(ticketMapper::toDTO).toList();
            }

            private TicketSort createSortFromMeta(Map<String, SortMeta> sortBy) {
                TicketSort sort = null;
                if (!sortBy.isEmpty()) {
                    SortMeta sortMeta = sortBy.values().iterator().next();
                    TicketSortField sortField = columnMapper.get(sortMeta.getField());
                    sort = new TicketSort(sortField, sortMeta.getOrder().isAscending());
                }
                return sort;
            }

            @Override
            public String getRowKey(TicketDTO ticketDTO) {
                return ticketDTO != null ? String.valueOf(ticketDTO.id()) : null;
            }

            @Override
            public TicketDTO getRowData(String rowKey) {
                List<TicketDTO> tickets = getWrappedData();
                if (tickets != null && rowKey != null) {
                    UUID id = UUID.fromString(rowKey);
                    return tickets.stream().filter(p -> id.equals(p.id())).findFirst().orElse(null);
                }
                return null;
            }
        };
    }

    @Nonnull
    private static String getEscapedValue(@Nonnull String inputValue) {
        return inputValue.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static TimeRange getDateRange(FilterMeta meta) {
        Object raw = meta.getFilterValue();
        if (raw instanceof List<?> range && range.size() == 2 && range.get(0) instanceof LocalDate start && range.get(
                1) instanceof LocalDate end) {
            Instant fromInclusive = start.atStartOfDay(TicketsView.EU_CH_ZONE_ID).toInstant();
            Instant toExclusive = end.plusDays(1).atStartOfDay(TicketsView.EU_CH_ZONE_ID).toInstant();
            return new TimeRange(fromInclusive, toExclusive);
        }
        return null;
    }

    public LazyDataModel<TicketDTO> getLazyModel() {
        return lazyModel;
    }

    public TicketPriority[] getTicketPriorities() {
        return TicketPriority.values();
    }

    public TicketStatus[] getTicketStatuses() {
        return TicketStatus.values();
    }

    public String formatDueAt(Instant dueAt) {
        return dueAt == null ? null : DUE_AT_FORMATTER.format(dueAt);
    }
}
