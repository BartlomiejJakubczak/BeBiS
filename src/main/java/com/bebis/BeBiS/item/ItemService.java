package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.domain.exception.InvalidItemException;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.event.ItemPersistedEvent;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ItemService.class);

    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    private final BlizzardItemFetcher itemFetcher;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final ItemEntityFactory itemEntityFactory;

    public ItemService(BlizzardItemFetcher itemFetcher, ItemRepository itemRepository, ItemMapper itemMapper,
                       ItemEntityFactory itemEntityFactory, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate) {
        this.itemFetcher = itemFetcher;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.itemEntityFactory = itemEntityFactory;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    public Map<EquipmentResponse.ItemDTO, ItemEntity> resolveItems(List<EquipmentResponse.ItemDTO> dtos) {
        Map<ItemEntity.CompositeKey, EquipmentResponse.ItemDTO> pkToRepresentativeDto = mapPksToDTOs(dtos);
        return mapDTOsToEntities(dtos, resolveItemsMissingInDb(fetchInitialEntities(pkToRepresentativeDto.keySet()), pkToRepresentativeDto));
    }

    private Map<ItemEntity.CompositeKey, ItemEntity> resolveItemsMissingInDb(Map<ItemEntity.CompositeKey, ItemEntity> initialEntities,
                                                                             Map<ItemEntity.CompositeKey, EquipmentResponse.ItemDTO> pkToRepresentativeDto) {
        Map<ItemEntity.CompositeKey, ItemEntity> resolvedEntities = new HashMap<>(initialEntities);
        Map<Long, ItemResponse> missingItems = new HashMap<>();
        for (Map.Entry<ItemEntity.CompositeKey, EquipmentResponse.ItemDTO> entry : pkToRepresentativeDto.entrySet()) {
            ItemEntity.CompositeKey pk = entry.getKey();
            if (!resolvedEntities.containsKey(pk)) {
                long baseItemId = pk.getBaseId();
                ItemResponse baseDTO = missingItems.computeIfAbsent(baseItemId, itemFetcher::fetchItem);
                Optional<ItemSyncData> syncData = mapToSyncData(baseDTO, entry.getValue());
                // TODO right now invalid dtos are ignored, in the future it could be dto -> null to signal that something went wrong
                syncData.ifPresent(itemSyncData -> resolvedEntities.put(pk, persist(itemSyncData)));
            }
        }
        return resolvedEntities;
    }

    private Map<ItemEntity.CompositeKey, ItemEntity> fetchInitialEntities(Set<ItemEntity.CompositeKey> pks) {
        return itemRepository.findAllById(pks)
                .stream()
                .collect(Collectors.toMap(ItemEntity::getPk, entity -> entity));
    }

    private Map<ItemEntity.CompositeKey, EquipmentResponse.ItemDTO> mapPksToDTOs(List<EquipmentResponse.ItemDTO> dtos) {
        return dtos.stream()
                .collect(Collectors.toMap(
                        this::toCompositeKey,
                        dto -> dto,
                        (existing, duplicate) -> existing // if there are two identical items equipped just pick one
                ));
    }

    private Map<EquipmentResponse.ItemDTO, ItemEntity> mapDTOsToEntities(
            List<EquipmentResponse.ItemDTO> dtos, Map<ItemEntity.CompositeKey, ItemEntity> resolvedEntities) {
        // because pkToRepresentativeDto holds unique entries you have to iterate over "dtos" that holds info on ALL items
        // that are equipped, even the duplicates.
        return dtos.stream()
                .filter((dto) -> resolvedEntities.containsKey(toCompositeKey(dto)))
                // TODO right now invalid dtos are ignored, in the future it could be dto -> null to signal that something went wrong
                .collect(Collectors.toMap(
                        dto -> dto,
                        dto -> resolvedEntities.get(toCompositeKey(dto)),
                        (existing, duplicate) -> existing
                ));
    }

    private ItemEntity.CompositeKey toCompositeKey(EquipmentResponse.ItemDTO dto) {
        return new ItemEntity.CompositeKey(dto.item().id(), itemMapper.mapSuffixId(dto));
    }

    // No transactional as this method is private, so Spring's AOP doesn't work
    private ItemEntity persist(ItemSyncData syncData) {
        return transactionTemplate.execute((s) -> {
            ItemEntity persistedEntity = itemRepository.save(itemEntityFactory.createItemEntity(syncData));
            // event publishing also has to be inside the transaction for any @TransactionalEventListener listening.
            eventPublisher.publishEvent(new ItemPersistedEvent(persistedEntity.getPk().getBaseId(), persistedEntity.getPk().getSuffixId(), Instant.now()));
            return persistedEntity;
        });
    }

    private Optional<ItemSyncData> mapToSyncData(@NonNull ItemResponse baseDTO, @NonNull EquipmentResponse.ItemDTO equippedItemDTO) {
        try {
            return Optional.of(itemMapper.mapToSyncData(baseDTO, equippedItemDTO));
        } catch (InvalidItemException e) {
            log.error("Invalid item data, reason: {}", e.getMessage());
            return Optional.empty();
        }
    }

}
