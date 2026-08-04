package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.ItemMapper;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.tools.EnumTools;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class EquipmentMapper {

    private final ItemMapper itemMapper;

    public EquipmentMapper(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    public Equipment mapToDomain(EquipmentEntity equipmentEntity) {
        Equipment equipment = new Equipment();
        equipmentEntity.getItems().forEach((slot, equippedEntity) -> {
            Item item = itemMapper.mapToDomain(equippedEntity.getItem());
            equipment.putItem(slot, item, equippedEntity.getPlayerEnchants());
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
