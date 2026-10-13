package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
final class SingleSlotUpgradeFinder extends UpgradeFinder {

    private final UpgradeResultEvaluator evaluator;

    SingleSlotUpgradeFinder(PowerCalculatorEngine calculator, UpgradeResultEvaluator evaluator) {
        super(calculator);
        this.evaluator = evaluator;
    }

    UpgradeResult findUpgrade(SingleSlotUpgradeOrchestrator.SlotUpgradeQuery query, ClassSpec classSpec) {
        List<ScoredItem> scoredItems = scoreAndSortItems(query.itemPool(), classSpec);
        return evaluator.evaluateUpgradeResult(extractEquippedItemFromPool(query.currentItem(), scoredItems), scoredItems.stream().findFirst());
    }

}
