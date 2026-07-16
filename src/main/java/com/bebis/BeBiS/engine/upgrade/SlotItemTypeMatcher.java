package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.equipment.domain.Equipment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;
import static com.bebis.BeBiS.item.domain.Item.InventoryType;


@Component
class SlotItemTypeMatcher {

    private final Map<Equipment.Slot, Set<InventoryType>> slotToInventoryType = Map.ofEntries(
            Map.entry(HEAD, Set.of(InventoryType.HEAD)),
            Map.entry(NECK, Set.of(InventoryType.NECK)),
            Map.entry(SHOULDER, Set.of(InventoryType.SHOULDER)),
            Map.entry(BACK, Set.of(InventoryType.CLOAK)),
            Map.entry(CHEST, Set.of(InventoryType.CHEST)),
            Map.entry(WRIST, Set.of(InventoryType.WRIST)),
            Map.entry(HANDS, Set.of(InventoryType.HANDS)),
            Map.entry(WAIST, Set.of(InventoryType.WAIST)),
            Map.entry(LEGS, Set.of(InventoryType.LEGS)),
            Map.entry(FEET, Set.of(InventoryType.FEET)),
            Map.entry(FINGER_1, Set.of(InventoryType.FINGER)),
            Map.entry(FINGER_2, Set.of(InventoryType.FINGER)),
            Map.entry(TRINKET_1, Set.of(InventoryType.TRINKET)),
            Map.entry(TRINKET_2, Set.of(InventoryType.TRINKET)),
            Map.entry(MAIN_HAND, Set.of(InventoryType.WEAPON, InventoryType.WEAPONMAINHAND, InventoryType.TWO_HAND)),
            Map.entry(OFF_HAND, Set.of(InventoryType.WEAPON, InventoryType.WEAPONOFFHAND, InventoryType.HOLDABLE, InventoryType.SHIELD)),
            Map.entry(RANGED, Set.of(InventoryType.RANGED, InventoryType.RANGEDRIGHT, InventoryType.THROWN))
    );

    Set<InventoryType> matchSlotToItemTypes(Equipment.Slot slot) {
        return slotToInventoryType.get(slot);
    }
}
