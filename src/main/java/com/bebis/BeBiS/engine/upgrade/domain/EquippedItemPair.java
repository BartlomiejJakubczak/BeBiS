package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;

import java.util.Optional;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.MAIN_HAND;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.OFF_HAND;

public record EquippedItemPair(Optional<EquippedItem> slot1, Optional<EquippedItem> slot2) {

    public static EquippedItemPair from(InterchangeableSlots slots, Equipment equipment) {
        return new EquippedItemPair(equipment.getItem(slots.slot1()), equipment.getItem(slots.slot2()));
    }

    public static EquippedItemPair handSlotsPair(Equipment equipment) {
        return new EquippedItemPair(equipment.getItem(MAIN_HAND), equipment.getItem(OFF_HAND));
    }
}
