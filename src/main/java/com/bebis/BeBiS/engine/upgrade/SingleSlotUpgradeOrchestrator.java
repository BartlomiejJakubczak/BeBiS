package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public void findUpgrade(Equipment.Slot slot, CharacterInfo info) {
        List<Item> itemPool = itemPoolProvider.getItemPool(slot, info.wowCharacter().level(), info.wowCharacter().wowClass());
        publisher.publish(slot, upgradeFinder.findUpgrade(info.equipment().getItem(slot), info.wowCharacter(), info.talents(), itemPool));
    }

}
