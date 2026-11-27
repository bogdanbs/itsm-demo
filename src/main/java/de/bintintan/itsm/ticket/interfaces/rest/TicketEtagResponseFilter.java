package de.bintintan.itsm.ticket.interfaces.rest;

import de.bintintan.itsm.ticket.interfaces.dto.TicketDTO;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.RuntimeDelegate;

@Provider
public class TicketEtagResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (response.getEntity() instanceof TicketDTO dto) {
            String value = "ticket-" + dto.id() + "-v" + dto.version();
            response.getHeaders().putSingle(HttpHeaders.ETAG,
                    RuntimeDelegate.getInstance().createHeaderDelegate(EntityTag.class).toString(new EntityTag(value)));
        }
    }
}