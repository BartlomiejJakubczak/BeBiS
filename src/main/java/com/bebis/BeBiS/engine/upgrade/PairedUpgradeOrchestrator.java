package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.SlotPair;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.bebis.BeBiS.engine.upgrade.PairedUpgradeFinder.ItemPair;

@Component
class PairedUpgradeOrchestrator {

    private final ItemPoolProvider itemPoolProvider;
    private final PairedUpgradeFinder pairedUpgradeFinder;

    private final ApplicationEventPublisher publisher;

    PairedUpgradeOrchestrator(ItemPoolProvider itemPoolProvider, PairedUpgradeFinder pairedUpgradeFinder,
                              ApplicationEventPublisher publisher) {
        this.itemPoolProvider = itemPoolProvider;
        this.pairedUpgradeFinder = pairedUpgradeFinder;
        this.publisher = publisher;
    }

    @Async
    public void findUpgrades(SlotPair slots, CharacterInfo info) {
        // both paired slots go to the same InventoryType, hence the same itemPool for both slots
        List<Item> itemPool = itemPoolProvider.getItemPool(slots.slot1(), info.wowCharacter().level(), info.wowCharacter().wowClass());

        Map<Equipment.Slot, UpgradeResult> results = pairedUpgradeFinder.findUpgrades(ItemPair.from(slots, info.equipment()),
                info.wowCharacter(), info.talents(), itemPool);

        evaluateResults(results);
    }

    private void evaluateResults(Map<Equipment.Slot, UpgradeResult> results) {
        results.forEach((slot, result) -> {
            switch (result) {
                case UpgradeResult.UpgradeFoundResult r ->
                        publisher.publishEvent(new UpgradeEvent.UpgradeFoundEvent(slot, r.item(), r.timestamp()));
                case UpgradeResult.AlreadyBiSResult r ->
                        publisher.publishEvent(new UpgradeEvent.BiSAlreadyEquippedEvent(slot, r.item(), r.timestamp()));
                case UpgradeResult.NoItemsAvailableResult r ->
                        publisher.publishEvent(new UpgradeEvent.UpgradeNotAvailableEvent(slot, r.timestamp()));
            }
        });
    }

}
