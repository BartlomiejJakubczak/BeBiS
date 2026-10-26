package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.EquippableItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Component;

/**
 * Scores an item for a slot and character: weapons go to {@link WeaponScoreCalculator} (DPS, speed and racials depend
 * on the slot), everything else to {@link AttributeScoreCalculator}.
 */
@Component
public final class PowerCalculatorEngine {

    private final AttributeScoreCalculator attributeCalculator;
    private final WeaponScoreCalculator weaponCalculator;

    PowerCalculatorEngine(AttributeScoreCalculator attributeCalculator, WeaponScoreCalculator weaponCalculator) {
        this.attributeCalculator = attributeCalculator;
        this.weaponCalculator = weaponCalculator;
    }

    public double calculate(Slot slot, Item item, ClassSpec classSpec, WowCharacter.Race race) {
        return switch (item) {
            case Weapon w -> weaponCalculator.calculate(slot, w, classSpec, race);
            case Armor a -> attributeCalculator.calculate(a, classSpec); // could be a shield
            case EquippableItem e -> attributeCalculator.calculate(e, classSpec); // could be a holdable
        };
    }
}
