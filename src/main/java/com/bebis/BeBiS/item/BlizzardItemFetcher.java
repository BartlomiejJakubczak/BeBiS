package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class BlizzardItemFetcher {

    private final BlizzardServiceClient blizzardClient;

    public BlizzardItemFetcher(BlizzardServiceClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    @Cacheable(value = "items", key = "#id")
    public ItemResponse fetchItem(long id) {
        // This will now be intercepted by the Spring Proxy
        // because it will be called from ItemService (an external class).
        return blizzardClient.getBaseItem(id);
    }
}
