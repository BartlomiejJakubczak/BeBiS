package com.bebis.BeBiS.engine.upgrade.event;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.domain.Item;

import java.time.Instant;

public sealed interface UpgradeEvent {

    record BiSAlreadyEquippedEvent(Equipment.Slot slot, Instant timestamp) implements UpgradeEvent {
    }

    record UpgradeFoundEvent(Equipment.Slot slot, Item upgrade, Instant timestamp) implements UpgradeEvent {
    }

    record UpgradeNotAvailableEvent(Equipment.Slot slot, Instant timestamp) {
    }

}
