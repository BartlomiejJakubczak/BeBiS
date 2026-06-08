package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.ClassSpec;
import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.bebis.BeBiS.item.domain.StatType.WEAPON_DPS;
import static com.bebis.BeBiS.item.domain.StatType.WEAPON_SPEED;
import static com.bebis.BeBiS.item.domain.Weapon.WeaponType.*;
import static com.bebis.BeBiS.profile.domain.WowCharacter.Race.*;

@Component
public class PowerCalculatorEngine {

    public static final double RANGED_FLAT_BONUS = 20.0;
    public static final double MELEE_FLAT_BONUS = 80.0;

    private final StatWeightRegistry registry;

    private final Map<WowCharacter.Race, List<Weapon.WeaponType>> weaponRacials = Map.of(
            HUMAN, List.of(SWORD, MACE),
            DWARF, List.of(GUN),
            ORC, List.of(AXE),
            TROLL, List.of(BOW)
    );

    PowerCalculatorEngine(StatWeightRegistry registry) {
        this.registry = registry;
    }

    public double calculate(Item item, ClassSpec classSpec, WowCharacter.Race race) {
        StatWeights statWeights = registry.getWeightsFor(classSpec);

        double score = calculateStats(item, statWeights);

        score += switch (item) {
            case Weapon w -> calculateWeapon(w, statWeights, race);
            default -> 0.0;
        };

        return score;
    }

    private double calculateStats(Item item, StatWeights weights) {
        return item.getMetadata().stats().entrySet().stream()
                .mapToDouble((entry) -> weights.getWeightFor(entry.getKey()) * entry.getValue().doubleValue()).sum();
    }

    private double calculateWeapon(Weapon w, StatWeights statWeights, WowCharacter.Race race) {
        return calculateRacials(w, race) + calculateDpsAndSpeed(w, statWeights);
    }

    private double calculateRacials(Weapon w, WowCharacter.Race race) {
        double score = 0.0;
        List<Weapon.WeaponType> weaponTypes = weaponRacials.getOrDefault(race, List.of());
        if (weaponTypes.contains(w.getWeaponType())
                && (w.getWeaponType() == GUN || w.getWeaponType() == BOW)) score += RANGED_FLAT_BONUS;
        if (weaponTypes.contains(w.getWeaponType())
                && (w.getWeaponType() == SWORD || w.getWeaponType() == MACE || w.getWeaponType() == AXE))
            score += MELEE_FLAT_BONUS;
        return score;
    }

    private double calculateDpsAndSpeed(Weapon w, StatWeights statWeights) {
        double dpsScore = (((w.getMinDamage() + w.getMaxDamage()) / 2.0) / w.getSpeed()) * statWeights.getWeightFor(WEAPON_DPS);
        double speedScore = w.getSpeed() * statWeights.getWeightFor(WEAPON_SPEED);
        return dpsScore + speedScore;
    }
}
