package com.bebis.BeBiS.base;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;


@SpringBootTest
public abstract non-sealed class BaseResilienceTest extends BaseContainerTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetResilienceState() {
        // Reset the Circuit Breaker to "CLOSED" and clear stats
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);

        // Clear caches to ensure we always hit the resilience logic
        cacheManager.getCacheNames().forEach(name ->
                Optional.ofNullable(cacheManager.getCache(name)).ifPresent(Cache::clear));
    }
}
