package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotCombinations;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
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
import static com.bebis.BeBiS.engine.upgrade.HandSlotUpgradeOrchestrator.HandSlotItemPools;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.MAIN_HAND;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.OFF_HAND;

/**
 * Finds the best main hand and off hand combination among the combinations the class may use (two-hander alone, one
 * hand plus shield, dual wield, ...). The same weapon is scored separately for each hand, and an item that can be worn
 * only once never fills both hands.
 */
@Component
final class HandSlotUpgradeFinder {

    private final ItemScorer scorer;
    private final UpgradeResultEvaluator evaluator;

    HandSlotUpgradeFinder(ItemScorer scorer, UpgradeResultEvaluator evaluator) {
        this.scorer = scorer;
        this.evaluator = evaluator;
    }

    Map<Equipment.Slot, UpgradeResult> findUpgrades(EquippedItemPair currentPair, CharacterContext context, HandSlotCombinations combos, HandSlotItemPools itemPools) {
        ScoredItemPool scoredMainHands = scorer.scoreItemPool(MAIN_HAND, itemPools.mainHandPool(), context.classSpec(), context.race());
        ScoredItemPool scoredOffHands = scorer.scoreItemPool(OFF_HAND, itemPools.offHandPool(), context.classSpec(), context.race());

        SlotPair equipped = new SlotPair(scoredMainHands.getEquippedItem(currentPair.slot1().equippedItem()),
                scoredOffHands.getEquippedItem(currentPair.slot2().equippedItem()));

        Optional<SlotPair> bestCandidatePair = resolveComboWinner(equipped, evaluateCombos(combos, scoredMainHands, scoredOffHands));

        return bestCandidatePair.map((bestPair) ->
                bestPair.twoHandMainHand().map((twoHand) -> resolveIfCandidateTwoHand(equipped, twoHand))
                        .orElse(resolveIfCandidateNotTwoHand(equipped, bestPair))
        ).orElse(Map.of(MAIN_HAND, new UpgradeResult.NoItemsAvailableResult(Instant.now()),
                OFF_HAND, new UpgradeResult.NoItemsAvailableResult(Instant.now())));
    }

    private List<SlotPair> evaluateCombos(HandSlotCombinations combos, ScoredItemPool mainHandPool, ScoredItemPool offHandPool) {
        return combos.validHandSlotTypes().stream().map((types) -> {
            ScoredItemPool prunedMainHands = mainHandPool.extract(types.mainHand());
            ScoredItemPool prunedOffHands = offHandPool.extract(types.offHand());

            return mapCandidatePairs(prunedMainHands, prunedOffHands);
        }).flatMap(Collection::stream).toList();
    }

    private Optional<SlotPair> resolveComboWinner(SlotPair equipped, List<SlotPair> candidates) {
        return candidates.stream()
                .max(Comparator.comparingDouble(SlotPair::totalScore) // pick the best combos
                        .thenComparingInt((pair) -> pair.countEquippedMatches(equipped)) // favor pairs with equipped items
                        .thenComparing(Comparator.comparingInt(SlotPair::slotsEquippedCount).reversed()) // then look for pairs with the least slots occupied
                        .thenComparing(pair -> pair.slot1().map(s -> s.item().getMetadata().key().baseId()).orElse(0L)) // in case of tie in main hand
                        .thenComparing(pair -> pair.slot2().map(s -> s.item().getMetadata().key().baseId()).orElse(0L))); // in case of tie in off-hand
    }

    private List<SlotPair> mapCandidatePairs(ScoredItemPool prunedMainHands, ScoredItemPool prunedOffHands) {
        List<ScoredItem> mainHands = prunedMainHands.items();
        List<ScoredItem> offHands = prunedOffHands.items();
        Stream<SlotPair> candidateStream = offHands.isEmpty() // off-hands could be empty for combos like two-hand / null, but it should pair main-hands anyway
                ? mainHands.stream().map(main -> new SlotPair(Optional.of(main), Optional.empty()))
                : mainHands.stream().flatMap(main -> offHands.stream().map(off -> new SlotPair(Optional.of(main), Optional.of(off))));

        return candidateStream
                .filter(pair -> !pair.areSameOnceEquippables())
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
}
