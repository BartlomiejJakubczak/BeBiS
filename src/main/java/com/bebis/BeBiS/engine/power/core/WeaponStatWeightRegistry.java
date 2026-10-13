package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
final class WeaponStatWeightRegistry {

    private final Map<ClassSpec, StatWeights> registry = new EnumMap<>(ClassSpec.class);

    WeaponStatWeightRegistry() {
        populateRegistry();
    }

    StatWeights getWeightsFor(ClassSpec classSpec) {
        return registry.getOrDefault(classSpec, new StatWeights(new EnumMap<>(StatType.class)));
    }

    private void populateRegistry() {
// ==========================================
        // BASELINE CLASSES
        // ==========================================
        registry.put(ClassSpec.WARRIOR_NONE, weights(Map.of(
                StatType.TWO_HAND_DPS, 12.0,
                StatType.TWO_HAND_SPEED, 10.0,
                StatType.MAIN_HAND_DPS, 10.0,
                StatType.MAIN_HAND_SPEED, 5.0
        )));

        registry.put(ClassSpec.ROGUE_NONE, weights(Map.of(
                StatType.MAIN_HAND_DPS, 10.0,
                StatType.MAIN_HAND_SPEED, 5.0,
                StatType.OFF_HAND_DPS, 5.0,
                StatType.OFF_HAND_SPEED, -5.0
        )));

        registry.put(ClassSpec.HUNTER_NONE, weights(Map.of(
                StatType.RANGED_WEAPON_DPS, 15.0,
                StatType.RANGED_WEAPON_SPEED, 10.0
        )));

        registry.put(ClassSpec.PALADIN_NONE, weights(Map.of(
                StatType.TWO_HAND_DPS, 8.0,
                StatType.TWO_HAND_SPEED, 15.0,
                StatType.MAIN_HAND_DPS, 5.0
        )));

        registry.put(ClassSpec.SHAMAN_NONE, weights(Map.of(
                StatType.TWO_HAND_DPS, 8.0,
                StatType.TWO_HAND_SPEED, 10.0,
                StatType.MAIN_HAND_DPS, 5.0
        )));

        // Casters and Feral Druids omitted: in Classic 1.12, their spells and forms
        // do not scale with weapon DPS. Weapons function purely as stat sticks.

        // ==========================================
        // WARRIOR
        // ==========================================
        registry.put(ClassSpec.WARRIOR_ARMS, weights(Map.of(
                StatType.TWO_HAND_DPS, 14.0,
                StatType.TWO_HAND_SPEED, 25.0,   // Mortal Strike scales with slow, heavy hits
                StatType.MAIN_HAND_DPS, 10.0,
                StatType.MAIN_HAND_SPEED, 10.0
        )));

        registry.put(ClassSpec.WARRIOR_FURY, weights(Map.of(
                StatType.MAIN_HAND_DPS, 16.5,
                StatType.MAIN_HAND_SPEED, 12.0,  // Slow MH for Whirlwind/Cleave
                StatType.OFF_HAND_DPS, 8.25,     // Exactly 50% of MH DPS penalty
                StatType.OFF_HAND_SPEED, -5.0,   // Fast OH preferred for Flurry uptime and Rage
                StatType.TWO_HAND_DPS, 15.0,     // 2H Fury is viable in Classic
                StatType.TWO_HAND_SPEED, 15.0
        )));

        registry.put(ClassSpec.WARRIOR_PROTECTION, weights(Map.of(
                StatType.MAIN_HAND_DPS, 9.0,
                StatType.MAIN_HAND_SPEED, -15.0  // Fast MH is king for Heroic Strike queuing / Threat
        )));

        // ==========================================
        // ROGUE
        // ==========================================
        registry.put(ClassSpec.ROGUE_ASSASSINATION, weights(Map.of(
                StatType.MAIN_HAND_DPS, 12.0,
                StatType.MAIN_HAND_SPEED, 5.0,   // Daggers, slight preference for 1.8 over 1.4 for Mutilate/Backstab
                StatType.OFF_HAND_DPS, 6.0,
                StatType.OFF_HAND_SPEED, -15.0   // Ultra-fast OH for poison procs
        )));

        registry.put(ClassSpec.ROGUE_COMBAT, weights(Map.of(
                StatType.MAIN_HAND_DPS, 15.0,
                StatType.MAIN_HAND_SPEED, 20.0,  // Slow MH for Sinister Strike
                StatType.OFF_HAND_DPS, 7.5,
                StatType.OFF_HAND_SPEED, -15.0   // Ultra-fast OH for Combat Potency and poisons
        )));

        registry.put(ClassSpec.ROGUE_SUBTLETY, weights(Map.of(
                StatType.MAIN_HAND_DPS, 11.0,
                StatType.MAIN_HAND_SPEED, 25.0,  // Very slow MH for massive Ambush/Hemo
                StatType.OFF_HAND_DPS, 5.5,
                StatType.OFF_HAND_SPEED, -10.0
        )));

        // ==========================================
        // PALADIN
        // ==========================================
        registry.put(ClassSpec.PALADIN_RETRIBUTION, weights(Map.of(
                StatType.TWO_HAND_DPS, 11.0,
                StatType.TWO_HAND_SPEED, 35.0,   // Seal of Command requires ultra-slow 2H
                StatType.MAIN_HAND_DPS, 5.0
        )));

        registry.put(ClassSpec.PALADIN_PROTECTION, weights(Map.of(
                StatType.MAIN_HAND_DPS, 6.0,
                StatType.MAIN_HAND_SPEED, -15.0  // Fast MH for Seal of Righteousness threat
        )));

        // ==========================================
        // SHAMAN
        // ==========================================
        registry.put(ClassSpec.SHAMAN_ENHANCEMENT, weights(Map.of(
                StatType.TWO_HAND_DPS, 12.5,
                StatType.TWO_HAND_SPEED, 32.0,   // Windfury loves slow 2H weapons
                StatType.MAIN_HAND_DPS, 10.0,
                StatType.MAIN_HAND_SPEED, 20.0
        )));

        // ==========================================
        // HUNTER
        // ==========================================
        registry.put(ClassSpec.HUNTER_BEASTMASTERY, weights(Map.of(
                StatType.RANGED_WEAPON_DPS, 20.0,
                StatType.RANGED_WEAPON_SPEED, 15.0
        )));

        registry.put(ClassSpec.HUNTER_MARKSMANSHIP, weights(Map.of(
                StatType.RANGED_WEAPON_DPS, 28.0,
                StatType.RANGED_WEAPON_SPEED, 30.0
        )));

        registry.put(ClassSpec.HUNTER_SURVIVAL, weights(Map.of(
                StatType.RANGED_WEAPON_DPS, 22.0,
                StatType.RANGED_WEAPON_SPEED, 20.0
        )));
    }

    private static StatWeights weights(Map<StatType, Double> map) {
        return new StatWeights(new EnumMap<>(map));
    }
}
