package de.bintintan.itsm.system.interfaces.rest;

import de.bintintan.itsm.system.DatabaseInfo;
import de.bintintan.itsm.system.SystemInfoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Zeigt, ob die Anwendung die Datenbank erreicht: GET /itsm-demo/api/info
 */
@Path("info")
public class InfoResource {

    @Inject
    SystemInfoService systemInfoService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DatabaseInfo info() {
        return systemInfoService.databaseInfo();
    }
}
