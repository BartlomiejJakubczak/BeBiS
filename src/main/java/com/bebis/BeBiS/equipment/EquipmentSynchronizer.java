package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class EquipmentSynchronizer {

    private final ItemService itemService;
    private final EquipmentMapper mapper;

    public EquipmentSynchronizer(ItemService itemService, EquipmentMapper mapper) {
        this.itemService = itemService;
        this.mapper = mapper;
    }

    public void synchronize(EquipmentResponse response, EquipmentEntity equipment) {
        equipment.getItems().clear(); // a fresh snapshot
        Map<EquipmentResponse.ItemDTO, ItemEntity> resolvedItems = itemService.resolveItems(response.equipment());
        resolvedItems.forEach((dto, entity) -> mapper.mapSlot(dto).ifPresent(slot -> {
            EquipmentEntity.EquippedItem equippedItem = new EquipmentEntity.EquippedItem();
            equippedItem.setItem(entity);
            equippedItem.setPlayerEnchants(mapper.mapPlayerEnchants(dto, entity.getPk().getSuffixId()));
            equipment.getItems().put(slot, equippedItem);
        }));
    }
}
