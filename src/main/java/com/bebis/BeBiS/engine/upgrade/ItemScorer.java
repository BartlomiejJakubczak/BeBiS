package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import static com.bebis.BeBiS.profile.domain.WowCharacter.Race;

/**
 * Turns the candidates for a slot into a {@link ScoredItemPool}, scoring each item with the
 * {@link PowerCalculatorEngine} for one character. The finders depend on this instead of the calculator, so they only
 * decide which scored items win.
 */
@Component
final class ItemScorer {

    private final PowerCalculatorEngine calculator;

    ItemScorer(PowerCalculatorEngine calculator) {
        this.calculator = calculator;
    }

    /**
     * @param slot the slot the items are scored for, e.g. a weapon's DPS counts differently in main hand, off hand and
     *             ranged
     */
    ScoredItemPool scoreItemPool(Slot slot, ItemPool itemPool, ClassSpec classSpec, Race race) {
        List<ScoredItem> scoredPool = itemPool.items().stream()
                .map(it -> scoreItem(slot, it, classSpec, race))
                .toList();
        return new ScoredItemPool(scoredPool);
    }

    private ScoredItem scoreItem(Slot slot, Item item, ClassSpec classSpec, Race race) {
        return new ScoredItem(item, calculator.calculate(slot, item, classSpec, race));
    }
}
