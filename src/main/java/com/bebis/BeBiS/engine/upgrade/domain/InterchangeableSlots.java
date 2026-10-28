package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.equipment.domain.Equipment.Slot;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pair of rings or trinkets. They are interchangeable in a sense that a ring or trinket can be assigned to either of the slots,
 * but the enum itself states explicitly positions of the slots themselves, which is important for the Finders.
 */
public enum InterchangeableSlots {
    FINGERS(Slot.FINGER_1, Slot.FINGER_2),
    TRINKETS(Slot.TRINKET_1, Slot.TRINKET_2);

    private final Slot slot1;
    private final Slot slot2;

    InterchangeableSlots(Slot slot1, Slot slot2) {
        this.slot1 = slot1;
        this.slot2 = slot2;
    }

    public Slot slot1() {
        return slot1;
    }

    public Slot slot2() {
        return slot2;
    }

    /**
     * Trinkets and rings share the same InventoryType regardless of slot, so it doesn't matter which gets picked.
     */
    public Slot pickSlotForScoring() {
        return slot1;
    }

    public Set<Slot> slots() {
        return EnumSet.of(slot1, slot2);
    }

    public static Set<Slot> allSlots() {
        return Arrays.stream(InterchangeableSlots.values())
                .flatMap(slots -> slots.slots().stream())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Slot.class)));
    }
}
