package com.bebis.BeBiS.engine.upgrade.event;

import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.item.domain.Item;

import java.time.Instant;

public sealed interface UpgradeEvent {

    /**
     * @return the slot this event is about. Each record's {@code slot} component implements it.
     */
    Slot slot();

    record BiSAlreadyEquippedEvent(Slot slot, Item bis, Instant timestamp) implements UpgradeEvent {
    }

    record UpgradeFoundEvent(Slot slot, Item upgrade, Instant timestamp) implements UpgradeEvent {
    }

    record UpgradeNotAvailableEvent(Slot slot, Instant timestamp) implements UpgradeEvent {
    }

    record SlotClearedEvent(Slot slot, Item triggeringItem, Instant timestamp) implements UpgradeEvent {
    }

}
