package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.item.ItemQueryService;
import com.bebis.BeBiS.item.domain.Item;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EquipmentService {

    private final BlizzardUserClient blizzardUserClient;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentSynchronizer equipmentSynchronizer;
    private final ItemQueryService itemQueryService;

    public EquipmentService(
            BlizzardUserClient blizzardUserClient,
            EquipmentMapper equipmentMapper,
            EquipmentSynchronizer equipmentSynchronizer,
            ItemQueryService itemQueryService) {
        this.blizzardUserClient = blizzardUserClient;
        this.equipmentMapper = equipmentMapper;
        this.equipmentSynchronizer = equipmentSynchronizer;
        this.itemQueryService = itemQueryService;
    }

    /**
     * Returns the stored equipment with fully assembled items. Read-only: it doesn't call Blizzard, so call
     * {@link #syncEquipment} first for an up-to-date snapshot.
     */
    public Equipment getEquipmentForCharacter(EquipmentEntity currentEquipment) {
        Map<Item.ItemKey, Item> sourcedItemsByKey = itemQueryService.findAllByKeys(extractKeys(currentEquipment));
        return equipmentMapper.mapToDomain(currentEquipment, sourcedItemsByKey);
    }

    /**
     * Replaces the stored equipment snapshot with the character's current gear from Blizzard. Must run inside a
     * transaction, because {@code equipmentEntity} is updated as a managed entity, without an explicit save.
     */
    public void syncEquipment(String characterName, String realmSlug, EquipmentEntity equipmentEntity) {
        equipmentSynchronizer.synchronize(blizzardUserClient.getCharacterEquipment(realmSlug, characterName), equipmentEntity);
    }

    private Set<Item.ItemKey> extractKeys(EquipmentEntity equipment) {
        return equipment.getItems().values().stream()
                .map((eqIt) -> eqIt.getItem().getPk().toItemKey())
                .collect(Collectors.toSet());
    }
}
