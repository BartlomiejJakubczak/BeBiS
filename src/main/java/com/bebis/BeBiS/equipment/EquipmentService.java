package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.item.ItemService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EquipmentService {

    private final BlizzardUserClient blizzardUserClient;
    private final ItemService itemService;
    private final EquipmentMapper equipmentMapper;

    public EquipmentService(BlizzardUserClient blizzardUserClient, ItemService itemService, EquipmentMapper equipmentMapper) {
        this.blizzardUserClient = blizzardUserClient;
        this.itemService = itemService;
        this.equipmentMapper = equipmentMapper;
    }

    public Equipment getEquipmentForCharacter(String realmSlug, String characterName) {
        Map<Equipment.Slot, Long> items = equipmentMapper.map(blizzardUserClient.getCharacterEquipment(realmSlug, characterName));
        Equipment equipment = new Equipment();
        for (Map.Entry<Equipment.Slot, Long> entry : items.entrySet()) {
            equipment.putItem(entry.getKey(), itemService.getItem(entry.getValue()));
        }
        return equipment;
    }

}
