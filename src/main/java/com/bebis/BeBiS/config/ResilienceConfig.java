package com.bebis.BeBiS.config;

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerConfigCustomizer blizzardFetcherCB() {
        var cfg = ResilienceConstants.BlizzardFetcher.INSTANCE;
        return CircuitBreakerConfigCustomizer.of(ResilienceConstants.BLIZZARD_FETCHER, builder -> builder
                .slidingWindowSize(cfg.windowSize)
                .failureRateThreshold(cfg.threshold)
                .waitDurationInOpenState(cfg.waitDuration));
    }

    @Bean
    public RetryConfigCustomizer blizzardFetcherRetry() {
        var cfg = ResilienceConstants.BlizzardFetcher.INSTANCE;
        return RetryConfigCustomizer.of(ResilienceConstants.BLIZZARD_FETCHER, builder -> builder
                .maxAttempts(cfg.maxRetries)
                .waitDuration(cfg.retryWait)
                .retryExceptions(RestClientException.class));
    }

    public static final class ResilienceConstants {

        public static final String BLIZZARD_FETCHER = "blizzardFetcher"; // annotation-friendly solution

        public enum BlizzardFetcher {
            INSTANCE(6, 50, Duration.ofSeconds(20), 2, Duration.ofMillis(500));

            public final int windowSize;
            public final float threshold;
            public final Duration waitDuration;
            public final int maxRetries;
            public final Duration retryWait;

            BlizzardFetcher(int windowSize, float threshold, Duration waitDuration, int maxRetries, Duration retryWait) {
                this.windowSize = windowSize;
                this.threshold = threshold;
                this.waitDuration = waitDuration;
                this.maxRetries = maxRetries;
                this.retryWait = retryWait;
            }
        }

    }
}
