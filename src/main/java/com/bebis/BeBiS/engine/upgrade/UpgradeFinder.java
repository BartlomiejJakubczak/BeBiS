package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

abstract class UpgradeFinder {

    private final PowerCalculatorEngine calculator;

    protected UpgradeFinder(PowerCalculatorEngine calculator) {
        this.calculator = calculator;
    }

    protected Optional<ScoredItem> extractEquippedItemFromPool(Optional<EquippedItem> equippedItem, List<ScoredItem> itemPool) {
        return equippedItem.flatMap(equipped -> itemPool.stream()
                .filter((scoredItem -> equipped.item().equals(scoredItem.item())))
                .findFirst());
    }

    protected List<ScoredItem> scoreAndSortItems(List<Item> itemPool, ClassSpec classSpec, WowCharacter.Race race) {
        return itemPool.stream()
                .map((item) -> scoreItem(item, classSpec, race))
                .sorted(Comparator.comparingDouble(ScoredItem::score).reversed()
                        .thenComparingLong(item -> item.item().getMetadata().key().baseId())) // guarantees determinism
                // high to low
                .toList();
    }

    protected ScoredItem scoreItem(Item item, ClassSpec classSpec, WowCharacter.Race race) {
        return new ScoredItem(item, calculator.calculate(item, classSpec, race));
    }

}
