package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.item.domain.Item;

import java.time.Instant;

sealed interface UpgradeResult {

    record UpgradeFoundResult(Item item, Instant timestamp) implements UpgradeResult {
    }

    record AlreadyBiSResult(Instant timestamp) implements UpgradeResult {
    }

    record NoItemsAvailableResult(Instant timestamp) implements UpgradeResult {
    }
}
