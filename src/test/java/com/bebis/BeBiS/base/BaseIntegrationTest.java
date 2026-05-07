package com.bebis.BeBiS.base;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.testcontainers.junit.jupiter.Container;

public class BaseIntegrationTest extends BaseDatabaseTest {

    @Container
    @ServiceConnection
    // checks the type of the container and based on that extracts and registers required properties to Spring env
    // because those containers are static Testcontainers follows the Shared Container Pattern, so they start only once
    protected static final RedisContainer redis = new RedisContainer("redis:7-alpine");

    @Autowired
    protected CacheManager cacheManager;

    @AfterEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

}
