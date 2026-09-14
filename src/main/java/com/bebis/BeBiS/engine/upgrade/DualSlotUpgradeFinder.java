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

    record EquippedItemPair(Item slot1, Item slot2) {

        record Item(Equipment.Slot slot, Optional<EquippedItem> equippedItem) {
        }

        static EquippedItemPair from(DualSlot slots, Equipment equipment) {
            return new EquippedItemPair(
                    new EquippedItemPair.Item(slots.slot1(), equipment.getItem(slots.slot1())),
                    new EquippedItemPair.Item(slots.slot2(), equipment.getItem(slots.slot2()))
            );
        }
    }

    private record SlotAssignment(Optional<ScoredItem> slot1, Optional<ScoredItem> slot2) {
    }

    private record OutcomeAssignment(Optional<ScoredItem> slot1, Optional<ScoredItem> slot2) {

        double totalScore() {
            return slot1().map(ScoredItem::score).orElse(0.0) + slot2().map(ScoredItem::score).orElse(0.0);
        }

        boolean hasUniqueConflict() {
            if (slot1().isPresent() && slot2().isPresent()) {
                Item slot1 = slot1().get().item();
                return slot1.equals(slot2().get().item()) && slot1.getMetadata().uniqueEquipped();
            }
            return false;
        }
    }

    public Map<Equipment.Slot, UpgradeResult> findUpgrades(EquippedItemPair currentPair, WowCharacter wowCharacter, Optional<WowTalents> talents, List<Item> itemPool) {
        List<ScoredItem> scoredItems = scoreAndSortItems(itemPool, ClassSpec.getClassSpec(wowCharacter.wowClass(), talents), wowCharacter.race());

        Optional<ScoredItem> firstCandidate = scoredItems.stream().findFirst();
        Optional<ScoredItem> secondCandidate = pickSecondCandidate(firstCandidate, scoredItems);

        Optional<ScoredItem> firstEquipped = extractEquippedItemFromPool(currentPair.slot1().equippedItem(), scoredItems);
        Optional<ScoredItem> secondEquipped = extractEquippedItemFromPool(currentPair.slot2().equippedItem(), scoredItems);

        SlotAssignment assignment = chooseOptimalCandidateAssignment(new SlotAssignment(firstEquipped, secondEquipped),
                new SlotAssignment(firstCandidate, secondCandidate));

        return Map.of(currentPair.slot1().slot(), evaluator.evaluateUpgradeResult(firstEquipped, assignment.slot1()),
                currentPair.slot2().slot(), evaluator.evaluateUpgradeResult(secondEquipped, assignment.slot2()));
    }

    private SlotAssignment chooseOptimalCandidateAssignment(SlotAssignment equipped, SlotAssignment candidate) {
        SlotAssignment configA = new SlotAssignment(candidate.slot1(), candidate.slot2());
        SlotAssignment configB = new SlotAssignment(candidate.slot2(), candidate.slot1());

        OutcomeAssignment configAOutcome = new OutcomeAssignment(resolveSlotWinner(equipped.slot1(), candidate.slot1()),
                resolveSlotWinner(equipped.slot2(), candidate.slot2()));
        OutcomeAssignment configBOutcome = new OutcomeAssignment(resolveSlotWinner(equipped.slot1(), candidate.slot2()),
                resolveSlotWinner(equipped.slot2(), candidate.slot1()));

        if (configAOutcome.hasUniqueConflict()) return configB;
        if (configBOutcome.hasUniqueConflict()) return configA;

        int configABetterThanB = Double.compare(configAOutcome.totalScore(), configBOutcome.totalScore());

        if (configABetterThanB == 0) {
            return configA;
        } // default to a config to remain deterministic

        return configABetterThanB == 1 ? configA : configB;
    }

    private Optional<ScoredItem> resolveSlotWinner(Optional<ScoredItem> equippedItem, Optional<ScoredItem> upgradeCandidate) {
        return equippedItem.map(ScoredItem::score).orElse(0.0) >= upgradeCandidate.map(ScoredItem::score).orElse(0.0) ? equippedItem : upgradeCandidate;
    }

    private Optional<ScoredItem> pickSecondCandidate(Optional<ScoredItem> firstCandidate, List<ScoredItem> scoredItems) {
        return firstCandidate.flatMap(item -> item.item().getMetadata().uniqueEquipped()
                ? pickExcluding(item, scoredItems)
                : Optional.of(item));
    }

    private Optional<ScoredItem> pickExcluding(ScoredItem excluded, List<ScoredItem> scoredItems) {
        return scoredItems.stream()
                .filter((it) -> !it.equals(excluded))
                .findFirst();
    }

}
