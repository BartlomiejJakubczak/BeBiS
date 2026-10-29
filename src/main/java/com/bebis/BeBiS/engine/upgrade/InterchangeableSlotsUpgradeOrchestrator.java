package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.InterchangeableSlots;
import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.ItemScoringContext;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Runs the upgrade search for the interchangeable slot pairs (rings, trinkets): builds one item pool for
 * all of them, restricts it per pair and publishes one result per slot.
 */
@Component
class InterchangeableSlotsUpgradeOrchestrator implements UpgradeOrchestrator {

    private final InterchangeableSlotUpgradeFinder upgradeFinder;
    private final ItemPoolProvider itemPoolProvider;
    private final UpgradeEventPublisher publisher;

    InterchangeableSlotsUpgradeOrchestrator(ItemPoolProvider itemPoolProvider, InterchangeableSlotUpgradeFinder upgradeFinder,
                                            UpgradeEventPublisher publisher) {
        this.itemPoolProvider = itemPoolProvider;
        this.upgradeFinder = upgradeFinder;
        this.publisher = publisher;
    }

    @Override
    @Async
    public void findUpgradesFor(CharacterInfo info) {
        ItemPool itemPool = itemPoolProvider.getItemPool(InterchangeableSlots.allSlots(),
                new ItemPoolProvider.Query(info.level(), info.race(), info.wowClass()));

        ClassSpec classSpec = info.classSpec();

        for (InterchangeableSlots slots : InterchangeableSlots.values()) {
            SlotUpgradeQuery query = new SlotUpgradeQuery(slots, EquippedItemPair.from(slots, info.equipment()),
                    itemPoolProvider.restrictToSlots(slots.slots(), itemPool));
            Map<Slot, UpgradeResult> results = upgradeFinder.findUpgrades(query, new ItemScoringContext(classSpec, info.race()));
            results.forEach(publisher::publish);
        }
    }

    record SlotUpgradeQuery(InterchangeableSlots slots, EquippedItemPair currentPair, ItemPool itemPool) {
    }
}
