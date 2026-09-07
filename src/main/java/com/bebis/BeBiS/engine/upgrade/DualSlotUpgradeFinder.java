package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.DualSlot;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
final class DualSlotUpgradeFinder extends UpgradeFinder {

    private final UpgradeResultEvaluator evaluator;

    DualSlotUpgradeFinder(PowerCalculatorEngine calculator, UpgradeResultEvaluator evaluator) {
        super(calculator);
        this.evaluator = evaluator;
    }

    record ItemPair(Item slot1, Item slot2) {

        record Item(Equipment.Slot slot, Optional<EquippedItem> equippedItem) {
        }

        static ItemPair from(DualSlot slots, Equipment equipment) {
            return new ItemPair(
                    new ItemPair.Item(slots.slot1(), equipment.getItem(slots.slot1())),
                    new ItemPair.Item(slots.slot2(), equipment.getItem(slots.slot2()))
            );
        }
    }

    public Map<Equipment.Slot, UpgradeResult> findUpgrades(ItemPair currentPair, WowCharacter wowCharacter, Optional<WowTalents> talents, List<Item> itemPool) {
        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);
        WowCharacter.Race race = wowCharacter.race();

        List<ScoredItem> scoredItems = scoreAndSortItems(itemPool, classSpec, race);

        // candidates
        Optional<ScoredItem> firstItem = scoredItems.stream().findFirst();
        Optional<ScoredItem> secondItem = pickSecondCandidate(firstItem, scoredItems);

        Optional<EquippedItem> firstEquipped = currentPair.slot1().equippedItem();
        Optional<EquippedItem> secondEquipped = currentPair.slot2().equippedItem();

        return Map.of(currentPair.slot1().slot(), evaluator.evaluateUpgradeResult(scoreEquipped(firstEquipped, classSpec, race), firstItem),
                currentPair.slot2().slot(), evaluator.evaluateUpgradeResult(scoreEquipped(secondEquipped, classSpec, race), secondItem));
    }

    private Optional<ScoredItem> scoreEquipped(Optional<EquippedItem> equippedItem, ClassSpec classSpec, WowCharacter.Race race) {
        return equippedItem.map(item -> scoreItem(item.item(), classSpec, race));
    }

    private Optional<ScoredItem> pickSecondCandidate(Optional<ScoredItem> firstItem, List<ScoredItem> scoredItems) {
        return firstItem.flatMap(item -> item.item().getMetadata().uniqueEquipped()
                ? pickExcluding(item, scoredItems)
                : Optional.of(item));
    }

    private Optional<ScoredItem> pickExcluding(ScoredItem excluded, List<ScoredItem> scoredItems) {
        return scoredItems.stream()
                .filter((it) -> !it.equals(excluded))
                .findFirst();
    }

}
