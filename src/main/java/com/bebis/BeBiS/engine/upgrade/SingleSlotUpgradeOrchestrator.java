package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class SingleSlotUpgradeOrchestrator {

    private final SingleSlotUpgradeFinder upgradeFinder;
    private final ItemPoolProvider itemPoolProvider;

    private final ApplicationEventPublisher publisher;

    SingleSlotUpgradeOrchestrator(SingleSlotUpgradeFinder upgradeFinder, ItemPoolProvider itemPoolProvider,
                                  ApplicationEventPublisher publisher) {
        this.upgradeFinder = upgradeFinder;
        this.itemPoolProvider = itemPoolProvider;
        this.publisher = publisher;
    }

    @Async
    public void findUpgrade(Equipment.Slot slot, CharacterInfo info) {
        List<Item> itemPool = itemPoolProvider.getItemPool(slot, info.wowCharacter().level(), info.wowCharacter().wowClass());
        switch (upgradeFinder.findUpgrade(info.equipment().getItem(slot), info.wowCharacter(), info.talents(), itemPool)) {
            case UpgradeResult.UpgradeFoundResult result ->
                    publisher.publishEvent(new UpgradeEvent.UpgradeFoundEvent(slot, result.item(), result.timestamp()));
            case UpgradeResult.AlreadyBiSResult result ->
                    publisher.publishEvent(new UpgradeEvent.BiSAlreadyEquippedEvent(slot, result.item(), result.timestamp()));
            case UpgradeResult.NoItemsAvailableResult result ->
                    publisher.publishEvent(new UpgradeEvent.UpgradeNotAvailableEvent(slot, result.timestamp()));
        }
    }

}
