package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntityFactory;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    public ItemEntity getOrCreateEntity(@NonNull Long itemId, @NonNull EquipmentResponse.ItemDTO equippedItemDTO) {
        return itemRepository.findById(new CompositeKey(itemId, itemMapper.mapSuffixId(equippedItemDTO)))
                .orElseGet(() -> {
                    ItemResponse baseDTO = blizzardClient.getBaseItem(itemId);
                    ItemSyncData syncData = itemMapper.mapToSyncData(baseDTO, equippedItemDTO);
                    ItemEntity entity = itemEntityFactory.createItemEntity(syncData);
                    return itemRepository.save(entity);
                });
    }


    public Optional<ItemEntity> getEntity(Long itemId, long enchId) {
        return itemRepository.findById(new CompositeKey(itemId, enchId));
    }

}
