package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.DualSlot;
import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotCombinations;
import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Runs the main hand and off hand upgrade search asynchronously: builds a pool per hand, looks up the class's allowed
 * combinations in {@link ClassHandSlotRegistry} and publishes one result per hand.
 */
@Component
class HandSlotUpgradeOrchestrator {

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

    @Async
    void findUpgrades(DualSlot slots, CharacterInfo info) {
        WowCharacter character = info.wowCharacter();
        ItemPoolProvider.Query providerQuery = new ItemPoolProvider.Query(character.level(), character.race(),
                character.wowClass());

        ItemPool mainHandItemPool = itemPoolProvider.getItemPool(Equipment.Slot.MAIN_HAND, providerQuery);
        ItemPool offHandItemPool = itemPoolProvider.getItemPool(Equipment.Slot.OFF_HAND, providerQuery);

        EquippedItemPair itemPair = EquippedItemPair.from(slots, info.equipment());
        HandSlotItemPools itemPools = new HandSlotItemPools(mainHandItemPool, offHandItemPool);
        HandSlotCombinations combos = registry.getHandSlotCombinationFor(character.wowClass(), character.level());
        CharacterContext context = new CharacterContext(ClassSpec.getClassSpec(character.wowClass(), info.talents()), character.race());

        Map<Equipment.Slot, UpgradeResult> results = finder.findUpgrades(itemPair, context, combos, itemPools);

        results.forEach(publisher::publish);
    }
    
    record CharacterContext(ClassSpec classSpec, WowCharacter.Race race) {
    }

    record HandSlotItemPools(ItemPool mainHandPool, ItemPool offHandPool) {
    }
}
