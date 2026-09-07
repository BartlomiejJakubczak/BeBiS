package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
class IsolatedUpgradeFinder extends UpgradeFinder {

    private final UpgradeResultEvaluator evaluator;

    IsolatedUpgradeFinder(PowerCalculatorEngine calculator, UpgradeResultEvaluator evaluator) {
        super(calculator);
        this.evaluator = evaluator;
    }

    public UpgradeResult findUpgrade(Optional<EquippedItem> equippedItem, WowCharacter wowCharacter, Optional<WowTalents> talents, List<Item> itemPool) {
        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);
        WowCharacter.Race race = wowCharacter.race();

        return evaluator.evaluateUpgradeResult(equippedItem.map(item ->
                scoreItem(item.item(), classSpec, race)), extractBestItemFromPool(itemPool, classSpec, race));
    }

}
