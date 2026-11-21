package de.bintintan.itsm.ticket.rest.mapper;

import de.bintintan.itsm.ticket.domain.Ticket;
import de.bintintan.itsm.ticket.rest.dto.TicketDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TicketMapper {
    // 1. Entity -> DTO
    TicketDTO toDTO(Ticket ticket);
}
