package com.itsm.rest;

import com.itsm.system.SystemInfoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Einfacher Health-Endpunkt für die Kubernetes-Probes.
 * Die Liveness-Probe prüft bewusst NICHT die Datenbank: Ein kurzer DB-Ausfall soll
 * nicht dazu führen, dass Kubernetes den Pod neu startet.
 */
@Path("health")
public class HealthResource {

    private static final Map<String, String> STATUS_UP = Map.of("status", "UP");
    private static final Map<String, String> STATUS_DATABASE_DOWN =
            Map.of("status", "DOWN", "reason", "Database cannot be reached");

    @Inject
    private SystemInfoService systemInfoService;

    @GET
    @Path("live")
    @Produces(MediaType.APPLICATION_JSON)
    public Response live() {
        return Response.ok().entity(STATUS_UP).build();
    }

    @GET
    @Path("ready")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ready() {
        if (!systemInfoService.databaseInfo().connected()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(STATUS_DATABASE_DOWN).build();
        }
        return Response.ok().entity(STATUS_UP).build();
    }
}
