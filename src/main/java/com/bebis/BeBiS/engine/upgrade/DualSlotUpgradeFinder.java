package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.SlotPair;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
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

    Map<Equipment.Slot, UpgradeResult> findUpgrades(EquippedItemPair currentPair, ClassSpec classSpec, List<Item> itemPool) {
        List<ScoredItem> scoredItems = scoreAndSortItems(itemPool, classSpec);

        SlotPair equipped = new SlotPair(extractEquippedItemFromPool(currentPair.slot1().equippedItem(), scoredItems),
                extractEquippedItemFromPool(currentPair.slot2().equippedItem(), scoredItems));

        Optional<SlotPair> bestCandidatePair = pickBestPair(equipped, mapCandidatePairs(scoredItems));

        return bestCandidatePair.map((best) ->
                        Map.of(currentPair.slot1().slot(), evaluator.evaluateUpgradeResult(equipped.slot1(), best.slot1()),
                                currentPair.slot2().slot(), evaluator.evaluateUpgradeResult(equipped.slot2(), best.slot2())))
                .orElse(Map.of(currentPair.slot1().slot(), new UpgradeResult.NoItemsAvailableResult(Instant.now()), currentPair.slot2().slot(),
                        new UpgradeResult.NoItemsAvailableResult(Instant.now())));
    }

    private List<SlotPair> mapCandidatePairs(List<ScoredItem> scoredItems) {
        if (scoredItems.size() == 1) {
            ScoredItem onlyItem = scoredItems.getFirst();
            if (onlyItem.item().getMetadata().uniqueEquipped()) {
                return List.of(new SlotPair(Optional.of(onlyItem), Optional.empty()), new SlotPair(Optional.empty(), Optional.of(onlyItem)));
            }
        }
        return scoredItems.stream().flatMap((slot1) ->
                        scoredItems.stream().map((slot2) -> new SlotPair(Optional.of(slot1), Optional.of(slot2))))
                .filter((pair) -> !pair.hasUniqueConflict())
                .toList();
    }

    private Optional<SlotPair> pickBestPair(SlotPair equipped, List<SlotPair> candidates) {
        return candidates.stream()
                .max(Comparator.comparingDouble(SlotPair::totalScore) // pick the best pair
                        .thenComparingInt((pair) -> countEquippedMatches(equipped, pair)) // favor pairs with equipped items
                        .thenComparing(pair -> pair.slot1().map(s -> s.item().getMetadata().key().baseId()).orElse(0L)) // in case of tie in slot 1
                        .thenComparing(pair -> pair.slot2().map(s -> s.item().getMetadata().key().baseId()).orElse(0L))); // in case of tie in slot 2
    }
}
