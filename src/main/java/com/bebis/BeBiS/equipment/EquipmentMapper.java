package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.item.ItemMapper;
import com.bebis.BeBiS.item.domain.Item;
import org.springframework.stereotype.Component;

@Component
public class EquipmentMapper {

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
}
