package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.SlotPair;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the best pair of rings or trinkets. Both slots draw from one pool, so the pair is evaluated as a whole and an
 * item that can be worn only once ({@link com.bebis.BeBiS.item.domain.Item#isEquippableOnce()}) never fills both.
 */
@Component
final class InterchangeableSlotUpgradeFinder {

    private final ItemScorer scorer;
    private final UpgradeResultEvaluator evaluator;

    InterchangeableSlotUpgradeFinder(ItemScorer scorer, UpgradeResultEvaluator evaluator) {
        this.scorer = scorer;
        this.evaluator = evaluator;
    }

    Map<Equipment.Slot, UpgradeResult> findUpgrades(InterchangeableSlotsUpgradeOrchestrator.SlotUpgradeQuery query, ClassSpec classSpec, WowCharacter.Race race) {
        ScoredItemPool itemPool = scorer.scoreItemPool(query.slots().pickSlotForScoring(), query.itemPool(), classSpec, race);

        SlotPair equipped = new SlotPair(query.currentPair().slot1().map(EquippedItem::item).flatMap(itemPool::findItem),
                query.currentPair().slot2().map(EquippedItem::item).flatMap(itemPool::findItem));

        Optional<SlotPair> bestCandidatePair = pickBestPair(equipped, mapCandidatePairs(itemPool));

        return bestCandidatePair.map((best) ->
                        Map.of(query.slots().slot1(), evaluator.evaluateUpgradeResult(equipped.slot1(), best.slot1()),
                                query.slots().slot2(), evaluator.evaluateUpgradeResult(equipped.slot2(), best.slot2())))
                .orElse(Map.of(query.slots().slot1(), new UpgradeResult.NoItemsAvailableResult(Instant.now()), query.slots().slot2(),
                        new UpgradeResult.NoItemsAvailableResult(Instant.now())));
    }

    private List<SlotPair> mapCandidatePairs(ScoredItemPool itemPool) {
        List<ScoredItem> scoredItems = itemPool.items();
        if (scoredItems.size() == 1) {
            ScoredItem onlyItem = scoredItems.getFirst();
            if (onlyItem.item().isEquippableOnce()) {
                return List.of(new SlotPair(Optional.of(onlyItem), Optional.empty()), new SlotPair(Optional.empty(), Optional.of(onlyItem)));
            }
        }
        return scoredItems.stream().flatMap((slot1) ->
                        scoredItems.stream().map((slot2) -> new SlotPair(Optional.of(slot1), Optional.of(slot2))))
                .filter(pair -> !pair.areSameOnceEquippables())
                .toList();
    }

    private Optional<SlotPair> pickBestPair(SlotPair equipped, List<SlotPair> candidates) {
        return candidates.stream()
                .max(Comparator.comparingDouble(SlotPair::totalScore) // pick the best pair
                        .thenComparingInt((pair) -> pair.countEquippedMatches(equipped)) // favor pairs with equipped items
                        .thenComparing(pair -> pair.slot1().map(s -> s.item().getMetadata().key().baseId()).orElse(0L)) // in case of tie in slot 1
                        .thenComparing(pair -> pair.slot2().map(s -> s.item().getMetadata().key().baseId()).orElse(0L))); // in case of tie in slot 2
    }
}
