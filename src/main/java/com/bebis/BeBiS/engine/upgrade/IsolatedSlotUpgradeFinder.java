package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ItemScoringContext;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import org.springframework.stereotype.Component;

/**
 * Finds the best item for a slot that doesn't interact with any other slot (head, chest, ranged, ...).
 */
@Component
final class IsolatedSlotUpgradeFinder {

    private final ItemScorer scorer;
    private final UpgradeResultEvaluator evaluator;

    IsolatedSlotUpgradeFinder(ItemScorer scorer, UpgradeResultEvaluator evaluator) {
        this.scorer = scorer;
        this.evaluator = evaluator;
    }

    UpgradeResult findUpgrade(IsolatedSlotUpgradeOrchestrator.SlotUpgradeQuery query, ItemScoringContext context) {
        ScoredItemPool scoredItems = scorer.scoreItemPool(query.slot(), query.itemPool(), context);
        return evaluator.evaluateUpgradeResult(query.currentItem().map(EquippedItem::item).flatMap(scoredItems::findItem), scoredItems.bestItem());
    }

}
