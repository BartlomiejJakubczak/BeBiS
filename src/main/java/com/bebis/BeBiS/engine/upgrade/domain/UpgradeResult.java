package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.domain.Item;

import java.time.Instant;

public sealed interface UpgradeResult {

    record UpgradeFoundResult(Item item, Instant timestamp) implements UpgradeResult {
    }

    record AlreadyBiSResult(Item item, Instant timestamp) implements UpgradeResult {
    }

    record NoItemsAvailableResult(Instant timestamp) implements UpgradeResult {
    }

    record SlotClearedResult(Item triggeringItem, Instant timestamp) implements UpgradeResult {
    }
}
