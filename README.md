# ITSM Demo – Jakarta EE 10 / JSF / PrimeFaces auf Kubernetes

Lern- und Vorzeigeprojekt: eine kleine IT-Service-Management-Anwendung (Tickets)
mit Jakarta EE 10 auf WildFly, PrimeFaces-Oberfläche und PostgreSQL, deployt auf minikube.

## Stand: Etappe 1 – Gerüst

- Maven-WAR-Projekt (Java 21, Jakarta EE 10 API, PrimeFaces 14)
- WildFly-34-Image mit PostgreSQL-Treiber als Modul und Datasource `java:jboss/datasources/ItsmDS`
- Kubernetes: Namespace `itsm`, PostgreSQL (Secret, PVC, Deployment, Service), Anwendung (Deployment, Service)
- Startseite (JSF/PrimeFaces, `@ViewScoped`, AJAX) zeigt den Datenbankstatus
- REST: `GET /itsm-demo/api/health`, `GET /itsm-demo/api/info`
- Unit-Test mit JUnit 5 und Mockito

## Projektstruktur

```
pom.xml
Dockerfile                  WildFly-Image (Treiber + Datasource + WAR)
docker/configure.cli        WildFly-CLI-Skript für Treiber und Datasource
k8s/                        Kubernetes-Manifeste (in Reihenfolge 00, 10, 20)
src/main/java/.../rest      JAX-RS
src/main/java/.../system    Services
src/main/java/.../web       JSF Backing Beans
src/main/webapp             XHTML-Seiten, web.xml, beans.xml
```

## Bauen und deployen (PowerShell)

```powershell
# 1. minikube starten (falls nicht schon läuft)
minikube start

# 2. Bauen und testen
mvn clean package

# 3. Image direkt in minikube bauen (kein Registry-Push nötig)
minikube image build -t itsm-demo:0.1.0 .

# 4. Deployen
kubectl apply -f k8s/

# 5. Warten, bis alles läuft
kubectl get pods -n itsm -w

# 6. Zugriff von Windows aus
kubectl port-forward -n itsm svc/itsm-demo 8080:8080
```

Dann im Browser: http://localhost:8080/itsm-demo/

## Neue Version deployen

```powershell
mvn clean package
minikube image build -t itsm-demo:0.1.0 .
kubectl rollout restart deployment/itsm-demo -n itsm
```

## Nützliche Befehle

```powershell
kubectl logs -n itsm deploy/itsm-demo -f          # WildFly-Log
kubectl describe pod -n itsm -l app=itsm-demo     # z.B. bei CrashLoopBackOff
kubectl exec -n itsm -it deploy/postgres -- psql -U itsm -d itsm
kubectl delete namespace itsm                     # alles entfernen (inkl. Daten)
```

## Geplante Etappen

1. Gerüst und Deployment auf minikube ✔
2. Domäne und Persistenz: Ticket-Entity (JPA), Service, Unit-Tests
3. Oberfläche: Ticketliste und Detailansicht mit PrimeFaces (DataTable, Dialoge)
4. REST-API und Automatisierung (Eskalation überfälliger Tickets per Timer)
5. Integrations- und Funktionstests
