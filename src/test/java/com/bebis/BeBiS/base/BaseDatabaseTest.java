package com.bebis.BeBiS.base;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class BaseDatabaseTest {

    @Container // Tells Testcontainers to start/stop this
    @ServiceConnection // Auto-configures Spring datasource to connect to this container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
