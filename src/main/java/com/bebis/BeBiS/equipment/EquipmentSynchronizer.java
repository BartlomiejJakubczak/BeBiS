package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EquipmentSynchronizer {

    private final ItemService itemService;

    public EquipmentSynchronizer(ItemService itemService) {
        this.itemService = itemService;
    }

    public void synchronize(EquipmentResponse response, EquipmentEntity equipment) {
        equipment.getItems().clear(); // a fresh snapshot
        for (EquipmentResponse.ItemDTO itemDTO : response.equipment()) {
            Optional<Equipment.Slot> slot = extractSlot(itemDTO);
            if (slot.isEmpty()) {
                continue; // Skip the rest of the loop for this item
            }

            ItemEntity baseItem = itemService.getOrCreateEntity(itemDTO.item().id(), itemDTO);

            EquipmentEntity.EquippedItem freshItem = new EquipmentEntity.EquippedItem();
            freshItem.setItem(baseItem);
            freshItem.setPlayerEnchants(itemDTO.getPlayerEnchantStrings());

            equipment.getItems().put(slot.get(), freshItem);
        }
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
