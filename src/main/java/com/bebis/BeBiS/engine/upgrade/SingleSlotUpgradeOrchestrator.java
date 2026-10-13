package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class SingleSlotUpgradeOrchestrator {

    private final SingleSlotUpgradeFinder upgradeFinder;
    private final ItemPoolProvider itemPoolProvider;
    private final UpgradeEventPublisher publisher;


    SingleSlotUpgradeOrchestrator(SingleSlotUpgradeFinder upgradeFinder, ItemPoolProvider itemPoolProvider,
                                  UpgradeEventPublisher publisher) {
        this.upgradeFinder = upgradeFinder;
        this.itemPoolProvider = itemPoolProvider;
        this.publisher = publisher;
    }

    @Async
    void findUpgrade(Slot slot, CharacterInfo info) {
        WowCharacter character = info.wowCharacter();

        List<Item> itemPool = itemPoolProvider.getItemPool(slot, character.level(), character.wowClass());

        publisher.publish(slot, upgradeFinder.findUpgrade(new SlotUpgradeQuery(info.equipment().getItem(slot), itemPool),
                ClassSpec.getClassSpec(character.wowClass(), info.talents())));
    }

    record SlotUpgradeQuery(Optional<EquippedItem> currentItem, List<Item> itemPool) {
    }
}
