package de.bintintan.itsm.system;

/**
 * Ergebnis der Datenbankprüfung.
 *
 * @param connected true, wenn eine Verbindung aufgebaut werden konnte
 * @param product   Datenbankprodukt und -version (oder null)
 * @param message   Fehlermeldung, falls keine Verbindung möglich war
 */
public record DatabaseInfo(boolean connected, String product, String message) {

    public static DatabaseInfo ok(String product) {
        return new DatabaseInfo(true, product, null);
    }

    public static DatabaseInfo failed(String message) {
        return new DatabaseInfo(false, null, message);
    }
}
