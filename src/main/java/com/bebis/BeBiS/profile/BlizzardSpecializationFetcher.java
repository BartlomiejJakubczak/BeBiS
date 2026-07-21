package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import static com.bebis.BeBiS.config.ResilienceConfig.ResilienceConstants.BLIZZARD_FETCHER;

@Component
class BlizzardSpecializationFetcher {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BlizzardSpecializationFetcher.class);

    private final BlizzardUserClient blizzardClient;

    BlizzardSpecializationFetcher(BlizzardUserClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    @Retry(name = BLIZZARD_FETCHER, fallbackMethod = "fetchSpecializationFallback")
    @CircuitBreaker(name = BLIZZARD_FETCHER)
    @Cacheable(value = "specs", key = "#realmSlug + '_' + #characterName") // SpEL parser at work
    public SpecializationResponse fetchSpecialization(String realmSlug, String characterName) {
        // This will now be intercepted by the Spring Proxy
        // because it will be called from ItemService (an external class).
        return blizzardClient.getCharacterSpecialization(realmSlug, characterName);
    }

    private SpecializationResponse fetchSpecializationFallback(String realmSlug, String characterName, Exception e) {
        log.error("Blizzard API failed for specialization for character {} with realm slug {}. Reason: {}", characterName, realmSlug, e.getMessage());
        return new SpecializationResponse(null, null, null);
    }
}
