package com.itsm.system;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Prüft die Verbindung zur Datenbank über die in WildFly
 * konfigurierte Datasource (siehe docker/configure.cli).
 */
@ApplicationScoped
public class SystemInfoService {

    @Resource(lookup = "java:jboss/datasources/ItsmDS")
    DataSource dataSource;

    public DatabaseInfo databaseInfo() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            return DatabaseInfo.ok(meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        } catch (SQLException e) {
            return DatabaseInfo.failed(e.getMessage());
        }
    }
}
