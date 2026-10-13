package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotCombinations;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.SlotPair;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.bebis.BeBiS.engine.upgrade.HandSlotUpgradeOrchestrator.CharacterContext;
import static com.bebis.BeBiS.engine.upgrade.HandSlotUpgradeOrchestrator.WeaponItemPools;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.MAIN_HAND;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.OFF_HAND;
import static com.bebis.BeBiS.item.domain.Item.InventoryType;

@Component
final class HandSlotUpgradeFinder extends UpgradeFinder {

    private final UpgradeResultEvaluator evaluator;

    HandSlotUpgradeFinder(PowerCalculatorEngine calculator, UpgradeResultEvaluator evaluator) {
        super(calculator);
        this.evaluator = evaluator;
    }

    Map<Equipment.Slot, UpgradeResult> findUpgrades(EquippedItemPair currentPair, CharacterContext context, HandSlotCombinations combos, WeaponItemPools itemPools) {
        List<ScoredItem> scoredMainHands = scoreAndSortItems(MAIN_HAND, itemPools.mainHandPool(), context.classSpec(), context.race());
        List<ScoredItem> scoredOffHands = scoreAndSortItems(OFF_HAND, itemPools.offHandPool(), context.classSpec(), context.race());

        SlotPair equipped = new SlotPair(extractEquippedItemFromPool(currentPair.slot1().equippedItem(), scoredMainHands),
                extractEquippedItemFromPool(currentPair.slot2().equippedItem(), scoredOffHands));

        Optional<SlotPair> bestCandidatePair = resolveComboWinner(equipped, evaluateCombos(combos, scoredMainHands, scoredOffHands));

        return bestCandidatePair.map((bestPair) ->
                bestPair.twoHandMainHand().map((twoHand) -> resolveIfCandidateTwoHand(equipped, twoHand))
                        .orElse(resolveIfCandidateNotTwoHand(equipped, bestPair))
        ).orElse(Map.of(MAIN_HAND, new UpgradeResult.NoItemsAvailableResult(Instant.now()),
                OFF_HAND, new UpgradeResult.NoItemsAvailableResult(Instant.now())));
    }

    private List<SlotPair> evaluateCombos(HandSlotCombinations combos, List<ScoredItem> mainHandPool, List<ScoredItem> offHandPool) {
        return combos.validHandSlotTypes().stream().map((types) -> {
            List<ScoredItem> prunedMainHands = pruneHandSlotItemPool(types.mainHand(), mainHandPool);
            List<ScoredItem> prunedOffHands = pruneHandSlotItemPool(types.offHand(), offHandPool);

            return mapCandidatePairs(prunedMainHands, prunedOffHands);
        }).flatMap(Collection::stream).toList();
    }

    private Optional<SlotPair> resolveComboWinner(SlotPair equipped, List<SlotPair> candidates) {
        return candidates.stream()
                .max(Comparator.comparingDouble(SlotPair::totalScore) // pick the best combos
                        .thenComparingInt((pair) -> countEquippedMatches(equipped, pair)) // favor pairs with equipped items
                        .thenComparing(Comparator.comparingInt(SlotPair::slotsEquippedCount).reversed()) // then look for pairs with the least slots occupied
                        .thenComparing(pair -> pair.slot1().map(s -> s.item().getMetadata().key().baseId()).orElse(0L)) // in case of tie in main hand
                        .thenComparing(pair -> pair.slot2().map(s -> s.item().getMetadata().key().baseId()).orElse(0L))); // in case of tie in off-hand
    }

    private List<SlotPair> mapCandidatePairs(List<ScoredItem> prunedMainHands, List<ScoredItem> prunedOffHands) {
        Stream<SlotPair> candidateStream = prunedOffHands.isEmpty() // off-hands could be empty for combos like two-hand / null, but it should pair two-hands anyway
                ? prunedMainHands.stream().map(main -> new SlotPair(Optional.of(main), Optional.empty()))
                : prunedMainHands.stream().flatMap(main ->
                prunedOffHands.stream().map(off -> new SlotPair(Optional.of(main), Optional.of(off))));

        return candidateStream
                .filter(pair -> !pair.hasUniqueConflict())
                .toList();
    }

    private Map<Equipment.Slot, UpgradeResult> resolveIfCandidateNotTwoHand(SlotPair equipped, SlotPair bestCombo) {
        if (equipped.twoHandMainHand().isPresent()) {
            return Map.of(MAIN_HAND, bestCombo.slot1().isPresent() ? new UpgradeResult.UpgradeFoundResult(bestCombo.slot1().get().item(), Instant.now())
                            : new UpgradeResult.NoItemsAvailableResult(Instant.now()),
                    OFF_HAND, bestCombo.slot2().isPresent() ? new UpgradeResult.UpgradeFoundResult(bestCombo.slot2().get().item(), Instant.now())
                            : new UpgradeResult.NoItemsAvailableResult(Instant.now()));
        }
        return Map.of(MAIN_HAND, evaluator.evaluateUpgradeResult(equipped.slot1(), bestCombo.slot1()),
                OFF_HAND, evaluator.evaluateUpgradeResult(equipped.slot2(), bestCombo.slot2()));
    }

    private Map<Equipment.Slot, UpgradeResult> resolveIfCandidateTwoHand(SlotPair equipped, ScoredItem twoHand) {
        return Map.of(MAIN_HAND, evaluator.evaluateUpgradeResult(equipped.slot1(), Optional.of(twoHand)),
                OFF_HAND, equipped.slot2().isPresent()
                        ? new UpgradeResult.SlotClearedResult(twoHand.item(), Instant.now())
                        : new UpgradeResult.NoItemsAvailableResult(Instant.now()));
    }

    private List<ScoredItem> pruneHandSlotItemPool(List<InventoryType> targetTypes, List<ScoredItem> handSlotPool) {
        return handSlotPool.stream().filter((it) -> targetTypes.contains(it.item().getMetadata().inventoryType())).toList();
    }

}
