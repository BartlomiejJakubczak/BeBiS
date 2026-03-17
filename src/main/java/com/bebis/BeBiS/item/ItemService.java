package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final BlizzardServiceClient blizzardClient;

    public ItemService(BlizzardServiceClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    public Item getItem(long itemId) {
        var response = blizzardClient.getItem(itemId);
        return new Item(response.id(), response.name());
    }

}
