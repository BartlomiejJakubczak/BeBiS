package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Component;

/**
 * Finds the best item for a slot that doesn't interact with any other slot (head, chest, ranged, ...).
 */
@Component
final class SingleSlotUpgradeFinder {

    private final ItemScorer scorer;
    private final UpgradeResultEvaluator evaluator;

    SingleSlotUpgradeFinder(ItemScorer scorer, UpgradeResultEvaluator evaluator) {
        this.scorer = scorer;
        this.evaluator = evaluator;
    }

    UpgradeResult findUpgrade(SingleSlotUpgradeOrchestrator.SlotUpgradeQuery query, ClassSpec classSpec, WowCharacter.Race race) {
        ScoredItemPool scoredItems = scorer.scoreItemPool(query.slot(), query.itemPool(), classSpec, race);
        return evaluator.evaluateUpgradeResult(query.currentItem().map(EquippedItem::item).flatMap(scoredItems::findItem), scoredItems.bestItem());
    }

}
