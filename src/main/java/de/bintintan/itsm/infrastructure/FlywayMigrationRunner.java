package de.bintintan.itsm.infrastructure;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
public class FlywayMigrationRunner {

    @Resource(lookup = "java:jboss/datasources/ItsmDS")
    private DataSource dataSource;

    @PostConstruct
    public void migrate() {
        boolean enabled = Boolean.parseBoolean(System.getenv().getOrDefault("FLYWAY_ENABLED", "true"));
        if (!enabled) {
            return;
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas("itsm")
                .load();

        flyway.migrate();
    }
}
