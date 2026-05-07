package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntityFactory;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final BlizzardItemFetcher itemFetcher;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final ItemEntityFactory itemEntityFactory;

    public ItemService(BlizzardItemFetcher itemFetcher, ItemRepository itemRepository, ItemMapper itemMapper,
                       ItemEntityFactory itemEntityFactory) {
        this.itemFetcher = itemFetcher;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.itemEntityFactory = itemEntityFactory;
    }

    @Transactional
    public Map<EquipmentResponse.ItemDTO, ItemEntity> resolveItems(List<EquipmentResponse.ItemDTO> dtos) {
        Map<ItemEntity.CompositeKey, EquipmentResponse.ItemDTO> pkToRepresentativeDto = dtos.stream()
                .collect(Collectors.toMap(
                        this::toCompositeKey,
                        dto -> dto,
                        (existing, duplicate) -> existing // if there are two identical items equipped just pick one
                ));

        Map<ItemEntity.CompositeKey, ItemEntity> resolvedEntities = itemRepository.findAllById(pkToRepresentativeDto.keySet())
                .stream()
                .collect(Collectors.toMap(ItemEntity::getId, entity -> entity));

        Map<Long, ItemResponse> baseItems = new HashMap<>();

        for (Map.Entry<ItemEntity.CompositeKey, EquipmentResponse.ItemDTO> entry : pkToRepresentativeDto.entrySet()) {
            ItemEntity.CompositeKey pk = entry.getKey();
            if (!resolvedEntities.containsKey(pk)) {
                long baseItemId = pk.getItemId();
                ItemResponse baseDTO = baseItems.computeIfAbsent(baseItemId, itemFetcher::fetchItem);
                resolvedEntities.put(pk, mapAndPersist(baseDTO, entry.getValue()));
            }
        }

        // because pkToRepresentativeDto holds unique entries you have to iterate over "dtos" that holds info on ALL items
        // that are equipped, even the duplicates.
        return dtos.stream()
                .collect(Collectors.toMap(
                        dto -> dto,
                        dto -> resolvedEntities.get(toCompositeKey(dto)),
                        (existing, duplicate) -> existing
                ));
    }

    private ItemEntity.CompositeKey toCompositeKey(EquipmentResponse.ItemDTO dto) {
        return new ItemEntity.CompositeKey(dto.item().id(), itemMapper.mapSuffixId(dto));
    }

    private ItemEntity mapAndPersist(@NonNull ItemResponse baseDTO, @NonNull EquipmentResponse.ItemDTO equippedItemDTO) {
        ItemSyncData syncData = itemMapper.mapToSyncData(baseDTO, equippedItemDTO);
        ItemEntity entity = itemEntityFactory.createItemEntity(syncData);
        return itemRepository.save(entity);
    }

}
