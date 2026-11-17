package de.bintintan.itsm.web;

import de.bintintan.itsm.system.DatabaseInfo;
import de.bintintan.itsm.system.SystemInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Backing Bean der Startseite.
 * <p>
 * {@code @ViewScoped}: Die Bean lebt so lange, wie der Benutzer auf derselben
 * Seite bleibt. Der Zähler überlebt daher AJAX-Requests, wird aber bei einem
 * Neuladen der Seite zurückgesetzt. Mit {@code @RequestScoped} wäre er nach
 * jedem Klick wieder 0.
 */
@Named
@ViewScoped
public class StatusBean implements Serializable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Inject
    SystemInfoService systemInfoService;

    private DatabaseInfo databaseInfo;
    private int checks;
    private String lastCheck;

    @PostConstruct
    void init() {
        refresh();
    }

    public void refresh() {
        databaseInfo = systemInfoService.databaseInfo();
        checks++;
        lastCheck = LocalDateTime.now().format(TIME);

        FacesContext context = FacesContext.getCurrentInstance();
        if (checks > 1 && context != null) {
            FacesMessage.Severity severity = databaseInfo.connected()
                    ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_ERROR;
            String summary = databaseInfo.connected() ? "Datenbank erreichbar" : "Keine Verbindung";
            context.addMessage(null, new FacesMessage(severity, summary, lastCheck));
        }
    }

    public DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }

    public int getChecks() {
        return checks;
    }

    public String getLastCheck() {
        return lastCheck;
    }
}
