package com.bebis.BeBiS.base;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract sealed class BaseContainerTest permits BaseResilienceTest, BasePersistenceTest, BaseFullStackTest {

    @Container // Tells Testcontainers to start/stop this
    @ServiceConnection // Auto-configures Spring datasource to connect to this container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    // checks the type of the container and based on that extracts and registers required properties to Spring env
    // because those containers are static Testcontainers follows the Shared Container Pattern, so they start only once
    protected static final RedisContainer redis = new RedisContainer("redis:7-alpine");

}
