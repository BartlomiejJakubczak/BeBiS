package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


@Component
class IsolatedUpgradeFinder {

    private final PowerCalculatorEngine calculator;

    IsolatedUpgradeFinder(PowerCalculatorEngine calculator) {
        this.calculator = calculator;
    }

    public UpgradeResult findUpgrade(Optional<EquippedItem> equippedItem, WowCharacter wowCharacter, Optional<WowTalents> talents, List<Item> itemPool) {
        if (itemPool.isEmpty()) {
            return new UpgradeResult.NoItemsAvailableResult(Instant.now());
        }

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);
        WowCharacter.Race race = wowCharacter.race();

        ScoredItem upgrade = extractBestItemFromPool(itemPool, classSpec, race);

        if (equippedItem.isPresent()) {
            var currentScore = calculator.calculate(equippedItem.get().item(), classSpec, race);
            return upgrade.score > currentScore ? new UpgradeResult.UpgradeFoundResult(upgrade.item(), Instant.now()) : new UpgradeResult.AlreadyBiSResult(Instant.now());
        } else {
            return new UpgradeResult.UpgradeFoundResult(upgrade.item(), Instant.now());
        }
    }

    private ScoredItem extractBestItemFromPool(@NonNull List<Item> itemPool, ClassSpec classSpec, WowCharacter.Race race) {
        return itemPool.stream()
                .map(item -> new ScoredItem(item, calculator.calculate(item, classSpec, race)))
                .max(Comparator.comparingDouble(ScoredItem::score))
                .get();
    }

    private record ScoredItem(Item item, double score) {
    }
}
