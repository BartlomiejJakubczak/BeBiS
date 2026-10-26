package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.DualSlot;
import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
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
 * Runs the upgrade search for a pair of interchangeable slots (rings, trinkets) asynchronously and publishes one result
 * per slot.
 */
@Component
class DualSlotUpgradeOrchestrator {

    private final DualSlotUpgradeFinder upgradeFinder;
    private final ItemPoolProvider itemPoolProvider;
    private final UpgradeEventPublisher publisher;

    DualSlotUpgradeOrchestrator(ItemPoolProvider itemPoolProvider, DualSlotUpgradeFinder upgradeFinder,
                                UpgradeEventPublisher publisher) {
        this.itemPoolProvider = itemPoolProvider;
        this.upgradeFinder = upgradeFinder;
        this.publisher = publisher;
    }

    @Async
    void findUpgrades(DualSlot slots, CharacterInfo info) {
        WowCharacter character = info.wowCharacter();

        // both paired slots go to the same InventoryType, hence the same itemPool for both slots
        ItemPool itemPool = itemPoolProvider.getItemPool(slots.slot1(), character.level(), character.race(), character.wowClass());

        SlotUpgradeQuery query = new SlotUpgradeQuery(slots, EquippedItemPair.from(slots, info.equipment()), itemPool);

        Map<Equipment.Slot, UpgradeResult> results = upgradeFinder.findUpgrades(query,
                ClassSpec.getClassSpec(character.wowClass(), info.talents()), character.race());

        results.forEach(publisher::publish);
    }

    record SlotUpgradeQuery(DualSlot slots, EquippedItemPair currentPair, ItemPool itemPool) {
    }
}
