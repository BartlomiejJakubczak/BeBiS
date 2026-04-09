package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class EquipmentSynchronizer {

    private final ItemService itemService;

    public EquipmentSynchronizer(ItemService itemService) {
        this.itemService = itemService;
    }

    public void synchronize(EquipmentResponse response, EquipmentEntity equipment) {
        equipment.getItems().clear(); // a fresh snapshot
        Set<ItemEntity> toSave = new HashSet<>();
        for (EquipmentResponse.ItemDTO itemDTO : response.equipment()) {
            long suffixId = itemDTO.getSuffixId();
            ItemEntity baseItem = itemService.getOrCreateEntity(itemDTO.item().id(), suffixId);
            if (itemDTO.name() != null && suffixId != 0) {
                baseItem.setName(itemDTO.name()); // attach the full name to the item
            }
            EquipmentEntity.EquippedItem freshItem = new EquipmentEntity.EquippedItem();
            freshItem.setItem(baseItem);
            freshItem.setPlayerEnchants(itemDTO.getPlayerEnchantStrings());
            toSave.add(baseItem);
            extractSlot(itemDTO).ifPresent(value -> equipment.getItems().put(value, freshItem));
        }
        itemService.saveEntities(toSave);
    }

    private Optional<Equipment.Slot> extractSlot(EquipmentResponse.ItemDTO itemDTO) {
        try {
            // Convert "finger_1" -> "FINGER_1" to match your Enum exactly
            return Optional.of(Equipment.Slot.valueOf(itemDTO.slot().type().toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            // Log it: "Warning: Unknown slot type received from Blizzard: " + itemDTO.slot().type()
            return Optional.empty();
        }
    }
}
