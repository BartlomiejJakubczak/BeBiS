package com.bebis.BeBiS.items;

import org.springframework.stereotype.Service;

import com.bebis.BeBiS.integration.blizzard.BlizzardClient;

@Service
public class ItemsService {

    private final BlizzardClient blizzardClient;

    public ItemsService(BlizzardClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    public Item getItem(int itemId) {
        return blizzardClient.getItem(itemId);
    }
    
}
