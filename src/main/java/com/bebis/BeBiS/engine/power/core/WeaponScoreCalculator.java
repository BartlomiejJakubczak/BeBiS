package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.bebis.BeBiS.item.domain.Item.InventoryType.TWO_HAND;
import static com.bebis.BeBiS.item.domain.StatType.*;
import static com.bebis.BeBiS.item.domain.Weapon.WeaponType.*;
import static com.bebis.BeBiS.profile.domain.WowCharacter.Race.*;

@Component
final class WeaponScoreCalculator {

    private static final double RANGED_FLAT_BONUS = 20.0;
    private static final double MELEE_FLAT_BONUS = 80.0;

    private final WeaponStatWeightRegistry registry;

    private final AttributeScoreCalculator attributeCalculator;

    private static final Map<WowCharacter.Race, List<Weapon.WeaponType>> weaponRacials = Map.of(
            HUMAN, List.of(SWORD, MACE),
            DWARF, List.of(GUN),
            ORC, List.of(AXE),
            TROLL, List.of(BOW)
    );

    WeaponScoreCalculator(WeaponStatWeightRegistry registry, AttributeScoreCalculator attributeCalculator) {
        this.registry = registry;
        this.attributeCalculator = attributeCalculator;
    }

    public double calculate(Slot slot, Weapon weapon, ClassSpec classSpec, WowCharacter.Race race) {
        return calculateStats(weapon, classSpec) + calculateRacials(weapon, race) + calculateWeaponSpecifics(slot, weapon, registry.getWeightsFor(classSpec));
    }

    private double calculateStats(Weapon weapon, ClassSpec classSpec) {
        return attributeCalculator.calculate(weapon, classSpec);
    }

    private double calculateRacials(Weapon w, WowCharacter.Race race) {
        double score = 0.0;
        List<Weapon.WeaponType> weaponTypes = weaponRacials.getOrDefault(race, List.of());
        Weapon.WeaponType weaponType = w.getWeaponType();
        if (weaponTypes.contains(weaponType) && w.isRanged()) score += RANGED_FLAT_BONUS;
        if (weaponTypes.contains(weaponType) && !w.isRanged()) score += MELEE_FLAT_BONUS;
        return score;
    }

    private double calculateWeaponSpecifics(Slot slot, Weapon w, StatWeights statWeights) {
        if (slot == Slot.RANGED && w.isRanged()) {
            return calculateDpsAndSpeed(w, statWeights, RANGED_WEAPON_DPS, RANGED_WEAPON_SPEED);
        }
        if (slot == Slot.MAIN_HAND && w.getMetadata().inventoryType() == TWO_HAND) {
            return calculateDpsAndSpeed(w, statWeights, TWO_HAND_DPS, TWO_HAND_SPEED);
        }
        return slot == Slot.MAIN_HAND
                ? calculateDpsAndSpeed(w, statWeights, MAIN_HAND_DPS, MAIN_HAND_SPEED)
                : calculateDpsAndSpeed(w, statWeights, OFF_HAND_DPS, OFF_HAND_SPEED);
    }

    private double calculateDpsAndSpeed(Weapon w, StatWeights statWeights, StatType dpsType, StatType speedType) {
        return calculateDpsScore(w, statWeights, dpsType) + calculateSpeedScore(w, statWeights, speedType);
    }

    private double calculateDpsScore(Weapon w, StatWeights statWeights, StatType dpsType) {
        return (((w.getMinDamage() + w.getMaxDamage()) / 2.0) / w.getSpeed()) * statWeights.getWeightFor(dpsType);
    }

    private double calculateSpeedScore(Weapon w, StatWeights statWeights, StatType speedType) {
        return w.getSpeed() * statWeights.getWeightFor(speedType);
    }
}
