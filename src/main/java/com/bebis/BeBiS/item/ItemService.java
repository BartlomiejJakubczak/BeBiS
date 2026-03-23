package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final BlizzardServiceClient blizzardClient;
    private final ItemMapper itemMapper;

    public ItemService(BlizzardServiceClient blizzardClient, ItemMapper itemMapper) {
        this.blizzardClient = blizzardClient;
        this.itemMapper = itemMapper;
    }

    public Item getItem(long itemId) {
        var response = blizzardClient.getItem(itemId);
        return itemMapper.map(response);
    }

}
