# Copilot Instructions

## Build and test

- Requirements: Java 21, Maven, Docker/Minikube, and `kubectl` for Kubernetes deployment.
- Build the WAR and copy the PostgreSQL JDBC driver: `mvn clean package`
- Run the full test suite: `mvn test`
- Run one test class: `mvn -Dtest=SystemInfoServiceTest test`
- Run one test method: `mvn -Dtest=SystemInfoServiceTest#returnsProductWhenDatabaseIsReachable test`
- There is no separate lint or formatting Maven goal configured.

Deploy locally from Windows with the commands in `README.md`:

```powershell
minikube start
mvn clean package
minikube image build -t itsm-demo:0.1.0 .
kubectl apply -f k8s/
kubectl port-forward -n itsm svc/itsm-demo 8080:8080
```

The application is served at `http://localhost:8080/itsm-demo/`.

## Architecture

- Maven packages the application as `itsm-demo.war` for WildFly 42 (Jakarta EE 10), Java 21, and PrimeFaces 14.
- Runtime data is PostgreSQL. The JPA unit `itsmUnit` uses the WildFly datasource `java:jboss/datasources/ItsmDS`; `docker/configure.cli` creates that datasource and the PostgreSQL XA module.
- `ticket.domain` contains the ticket entity, status/priority rules, and repository port.
- `ticket.application` contains ticket use cases and application-level errors; `ticket.infrastructure.persistence` implements the repository port with JPA.
- System-status functionality is under `system`; its JAX-RS and JSF inbound adapters are in `system.interfaces.rest` and `system.interfaces.web`.
- `RestApplication` maps JAX-RS under `api`, so resources are exposed below `/itsm-demo/api/*`.
- `FlywayMigrationRunner` runs startup migrations from `src/main/resources/db/migration` in the `itsm` schema. Set `FLYWAY_ENABLED=false` to skip migrations.
- Database schema creation is disabled in `persistence.xml`; schema changes must be added as Flyway migrations.
- `k8s/` contains namespace, PostgreSQL Secret/PVC/deployment/service, and application deployment/service manifests in deployment order.
- Kubernetes liveness is `/itsm-demo/api/health/live`; readiness is `/itsm-demo/api/health/ready` and checks the database.

## Repository conventions

- Use the base package `de.bintintan.itsm`.
- Define JPA tables explicitly in the `itsm` PostgreSQL schema and store enum values as strings.
- Keep ticket state changes in `Ticket` domain methods. `TicketStatus.allowedNextStatuses()` is the transition table; invalid transitions use `InvalidStatusTransitionException`.
- Keep ticket use cases dependent on the `TicketRepository` port, not JPA. Persistence implementations belong in `ticket.infrastructure.persistence`.
- Use CDI for runtime components: `@ApplicationScoped` services, `@Named @ViewScoped` JSF backing beans, JAX-RS resources, and `@Inject` dependencies.
- Use JTA transactions in `@ApplicationScoped` application services by annotating the service or its mutating methods with `@Transactional`.
- When a service needs the database datasource, use the existing `java:jboss/datasources/ItsmDS` lookup pattern.
- JSF pages use PrimeFaces components and EL backing-bean properties. AJAX updates normally target the surrounding form.
- JUnit 5 test classes use Mockito with `@ExtendWith(MockitoExtension.class)`; package-private fields are currently injected directly in tests.
- Flyway migration filenames use `V<version>__<description>.sql`.
