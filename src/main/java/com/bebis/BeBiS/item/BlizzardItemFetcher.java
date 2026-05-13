package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import static com.bebis.BeBiS.config.ResilienceConfig.ResilienceConstants.BLIZZARD_FETCHER;

@Component
public class BlizzardItemFetcher {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BlizzardItemFetcher.class);

    private final BlizzardServiceClient blizzardClient;

    public BlizzardItemFetcher(BlizzardServiceClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    @Retry(name = BLIZZARD_FETCHER, fallbackMethod = "fetchItemFallback")
    @CircuitBreaker(name = BLIZZARD_FETCHER)
    @Cacheable(value = "items", key = "#id") // Cache hits skip the retry/CB logic
    public ItemResponse fetchItem(long id) {
        // This will now be intercepted by the Spring Proxy
        // because it will be called from ItemService (an external class).
        return blizzardClient.getBaseItem(id);
    }

    private ItemResponse fetchItemFallback(long id, Exception e) {
        log.error("Blizzard API failed for item {}. Reason: {}", id, e.getMessage());
        // Return a 'Null Object' or a minimal DTO so the UI can at least show a '?' icon
        return new ItemResponse(
                id, null, null, null, null, null, null, null, null
        );
    }
}
