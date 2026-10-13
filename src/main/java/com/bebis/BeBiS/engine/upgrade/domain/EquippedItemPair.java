package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;

import java.util.Optional;

public record EquippedItemPair(Item slot1, Item slot2) {

    public record Item(Equipment.Slot slot, Optional<EquippedItem> equippedItem) {
    }

    public static EquippedItemPair from(DualSlot slots, Equipment equipment) {
        return new EquippedItemPair(
                new Item(slots.slot1(), equipment.getItem(slots.slot1())),
                new Item(slots.slot2(), equipment.getItem(slots.slot2()))
        );
    }
}
