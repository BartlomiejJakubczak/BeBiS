package com.bebis.BeBiS.item;

import org.springframework.stereotype.Service;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;

@Service
public class ItemService {

    private final BlizzardServiceClient blizzardClient;

    public ItemService(BlizzardServiceClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    public Item getItem(int itemId) {
        return blizzardClient.getItem(itemId);
    }
    
}
