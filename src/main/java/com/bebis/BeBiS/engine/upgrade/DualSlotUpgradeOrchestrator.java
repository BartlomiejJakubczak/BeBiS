package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.DualSlot;
import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
        List<Item> itemPool = itemPoolProvider.getItemPool(slots.slot1(), character.level(), character.wowClass());

        Map<Equipment.Slot, UpgradeResult> results = upgradeFinder.findUpgrades(EquippedItemPair.from(slots, info.equipment()),
                ClassSpec.getClassSpec(character.wowClass(), info.talents()), itemPool);

        results.forEach(publisher::publish);
    }

}
