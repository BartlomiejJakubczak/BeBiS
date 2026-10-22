package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.tools.EnumTools;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
final class EquipmentMapper {

    /**
     * Places already assembled items into their slots, together with each slot's player enchants. The entity only says
     * which item sits where; what an item is comes from {@code ItemQueryService}.
     *
     * @param sourcedItems the equipped items by key, which must contain every item the entity references
     * @throws NullPointerException if an equipped item is missing from {@code sourcedItems}, which means the caller
     *                              didn't load it
     */
    public Equipment mapToDomain(EquipmentEntity equipmentEntity, Map<Item.ItemKey, Item> sourcedItems) {
        Equipment equipment = new Equipment();
        equipmentEntity.getItems().forEach((slot, equippedEntity) -> {
            equipment.putItem(slot,
                    Objects.requireNonNull(sourcedItems.get(equippedEntity.getItem().getPk().toItemKey()), "No assembled item for " + slot),
                    equippedEntity.getPlayerEnchants());
        });
        return equipment;
    }

    public List<String> mapPlayerEnchants(EquipmentResponse.ItemDTO dto, long suffixId) {
        if (dto.enchantments() == null) return List.of();
        return dto.enchantments().stream()
                .filter(ench -> ench.enchantmentId() != suffixId)
                .map(EquipmentResponse.ItemDTO.EnchantmentDTO::displayString)
                .toList();
    }

    public Optional<Equipment.Slot> mapSlot(EquipmentResponse.ItemDTO dto) {
        if (dto.slot() != null && !dto.slot().type().isEmpty()) {
            return EnumTools.fromString(Equipment.Slot.class, dto.slot().type());
        }
        return Optional.empty();
    }
}
