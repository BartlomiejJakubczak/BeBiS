package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotCombinations;
import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.ItemScoringContext;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.MAIN_HAND;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.OFF_HAND;

/**
 * Runs the main hand and off-hand upgrade search: builds a pool per hand, looks up the class's allowed
 * combinations in {@link ClassHandSlotRegistry} and publishes one result per hand.
 */
@Component
class HandSlotUpgradeOrchestrator implements UpgradeOrchestrator {

    private final ItemPoolProvider itemPoolProvider;
    private final ClassHandSlotRegistry registry;

    private final HandSlotUpgradeFinder finder;
    private final UpgradeEventPublisher publisher;

    HandSlotUpgradeOrchestrator(ItemPoolProvider itemPoolProvider, ClassHandSlotRegistry registry,
                                HandSlotUpgradeFinder finder, UpgradeEventPublisher publisher) {
        this.itemPoolProvider = itemPoolProvider;
        this.registry = registry;
        this.finder = finder;
        this.publisher = publisher;
    }

    @Override
    @Async
    public void findUpgradesFor(CharacterInfo info) {
        ItemPoolProvider.Query providerQuery = new ItemPoolProvider.Query(info.level(), info.race(), info.wowClass());

        ItemPool mainHandItemPool = itemPoolProvider.getItemPool(Set.of(MAIN_HAND), providerQuery);
        ItemPool offHandItemPool = itemPoolProvider.getItemPool(Set.of(OFF_HAND), providerQuery);

        EquippedItemPair equippedPair = EquippedItemPair.handSlotsPair(info.equipment());
        HandSlotItemPools itemPools = new HandSlotItemPools(mainHandItemPool, offHandItemPool);
        HandSlotCombinations combos = registry.getHandSlotCombinationFor(info.wowClass(), info.level());
        ItemScoringContext context = new ItemScoringContext(info.classSpec(), info.race());

        Map<Slot, UpgradeResult> results = finder.findUpgrades(equippedPair, context, combos, itemPools);

        results.forEach(publisher::publish);
    }

    record HandSlotItemPools(ItemPool mainHandPool, ItemPool offHandPool) {
    }
}
