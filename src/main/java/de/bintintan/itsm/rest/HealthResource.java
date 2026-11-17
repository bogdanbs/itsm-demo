package de.bintintan.itsm.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/**
 * Einfacher Health-Endpunkt für die Kubernetes-Probes.
 * Prüft bewusst NICHT die Datenbank: Ein kurzer DB-Ausfall soll
 * nicht dazu führen, dass Kubernetes den Pod neu startet.
 */
@Path("health")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
