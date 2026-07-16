package com.bebis.BeBiS.base;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

abstract sealed class BaseContainerTest permits BaseResilienceTest, BasePersistenceTest, BaseFullStackTest, BaseNonTransactionalFullstackTest {

    @ServiceConnection // Auto-configures Spring datasource to connect to this container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection
    // checks the type of the container and based on that extracts and registers required properties to Spring env
    // because those containers are static Testcontainers follows the Shared Container Pattern, so they start only once
    protected static final RedisContainer redis = new RedisContainer("redis:7-alpine");

    static {
        postgres.start();
        redis.start();
        // the stops will be taken care of by Ryuk container governed by Testcontainers' core.
    }

}
