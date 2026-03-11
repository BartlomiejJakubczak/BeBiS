package com.bebis.BeBiS.item;

import org.springframework.stereotype.Service;

import com.bebis.BeBiS.integration.blizzard.BlizzardClient;

@Service
public class ItemService {

    private final BlizzardClient blizzardClient;

    public ItemService(BlizzardClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    public Item getItem(int itemId) {
        return blizzardClient.getItem(itemId);
    }
    
}
