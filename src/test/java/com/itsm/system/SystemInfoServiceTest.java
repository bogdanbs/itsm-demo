package com.itsm.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemInfoServiceTest {

    @Mock
    DataSource dataSource;
    @Mock
    Connection connection;
    @Mock
    DatabaseMetaData metaData;

    SystemInfoService service;

    @BeforeEach
    void setUp() {
        service = new SystemInfoService();
        service.dataSource = dataSource;
    }

    @Test
    void returnsProductWhenDatabaseIsReachable() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getDatabaseProductVersion()).thenReturn("16.4");

        DatabaseInfo info = service.databaseInfo();

        assertTrue(info.connected());
        assertEquals("PostgreSQL 16.4", info.product());
        verify(connection).close(); // try-with-resources gibt die Verbindung zurück
    }

    @Test
    void returnsErrorWhenDatabaseIsDown() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        DatabaseInfo info = service.databaseInfo();

        assertFalse(info.connected());
        assertEquals("Connection refused", info.message());
    }
}
