package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntityFactory;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.bebis.BeBiS.item.jpa.ItemEntity.CompositeKey;

@Service
public class ItemService {

    private final BlizzardServiceClient blizzardClient;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final ItemEntityFactory itemEntityFactory;

    public ItemService(BlizzardServiceClient blizzardClient, ItemRepository itemRepository, ItemMapper itemMapper, ItemEntityFactory itemEntityFactory) {
        this.blizzardClient = blizzardClient;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.itemEntityFactory = itemEntityFactory;
    }

    @Transactional
    public ItemEntity getOrCreateEntity(Long itemId, long enchId) {
        return itemRepository.findById(new CompositeKey(itemId, enchId))
                .orElseGet(() -> {
                    ItemResponse dto = blizzardClient.getBaseItem(itemId);
                    ItemSyncData syncData = itemMapper.mapToSyncData(dto, enchId);
                    ItemEntity entity = itemEntityFactory.createItemEntity(syncData);
                    return itemRepository.save(entity);
                });
    }

    public void saveEntities(Set<ItemEntity> items) {
        itemRepository.saveAll(items);
    }
}
