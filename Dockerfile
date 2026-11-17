# WildFly 34 (Jakarta EE 10) auf Java 21
FROM quay.io/wildfly/wildfly:34.0.1.Final-jdk21

# 1) PostgreSQL-Treiber als Modul installieren und Datasource konfigurieren
COPY --chown=jboss:root target/dependency/postgresql.jar /tmp/postgresql.jar
COPY --chown=jboss:root docker/configure.cli /tmp/configure.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure.cli \
 && rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history \
           /opt/jboss/wildfly/standalone/data \
           /opt/jboss/wildfly/standalone/log

# 2) Anwendung deployen (eigene Schicht: ändert sich bei jedem Build)
COPY --chown=jboss:root target/itsm-demo.war /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8080
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
