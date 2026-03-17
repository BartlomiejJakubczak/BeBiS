package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final BlizzardServiceClient blizzardClient;

    public ItemService(BlizzardServiceClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    public ItemResponse getItem(long itemId) {
        return blizzardClient.getItem(itemId);
    }

}
