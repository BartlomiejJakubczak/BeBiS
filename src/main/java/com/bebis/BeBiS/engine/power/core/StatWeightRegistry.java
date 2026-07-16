package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

import static java.util.Map.entry;

@Component
class StatWeightRegistry {

    private final Map<ClassSpec, StatWeights> registry = new EnumMap<>(ClassSpec.class);

    public StatWeightRegistry() {
        populateRegistry();
    }

    public StatWeights getWeightsFor(ClassSpec classSpec) {
        return registry.getOrDefault(classSpec, new StatWeights(new EnumMap<>(StatType.class)));
    }

    private void populateRegistry() {
        // ==========================================
        // BASELINE CLASSES (Level 1-9 or unspent talents)
        // Balanced around raw leveling efficiency/momentum.
        // ==========================================
        registry.put(ClassSpec.WARRIOR_NONE, weights(Map.of(
                StatType.STRENGTH, 1.0,
                StatType.AGILITY, 0.6,
                StatType.STAMINA, 0.4,
                StatType.ARMOR, 0.01
        )));

        registry.put(ClassSpec.ROGUE_NONE, weights(Map.of(
                StatType.AGILITY, 1.0,
                StatType.STRENGTH, 0.6,
                StatType.STAMINA, 0.4,
                StatType.ARMOR, 0.01
        )));

        registry.put(ClassSpec.HUNTER_NONE, weights(Map.of(
                StatType.AGILITY, 1.0,
                StatType.INTELLECT, 0.4,
                StatType.STAMINA, 0.3,
                StatType.ARMOR, 0.01
        )));

        registry.put(ClassSpec.MAGE_NONE, weights(Map.of(
                StatType.INTELLECT, 1.0,
                StatType.SPIRIT, 0.7,
                StatType.STAMINA, 0.3,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.WARLOCK_NONE, weights(Map.of(
                StatType.INTELLECT, 1.0,
                StatType.STAMINA, 0.7,
                StatType.SPIRIT, 0.4,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.PRIEST_NONE, weights(Map.of(
                StatType.INTELLECT, 1.0,
                StatType.SPIRIT, 0.8,
                StatType.STAMINA, 0.3,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.PALADIN_NONE, weights(Map.of(
                StatType.STRENGTH, 1.0,
                StatType.STAMINA, 0.6,
                StatType.INTELLECT, 0.5,
                StatType.SPIRIT, 0.4,
                StatType.ARMOR, 0.01
        )));

        registry.put(ClassSpec.SHAMAN_NONE, weights(Map.of(
                StatType.STRENGTH, 1.0,
                StatType.STAMINA, 0.6,
                StatType.INTELLECT, 0.5,
                StatType.SPIRIT, 0.4,
                StatType.ARMOR, 0.01
        )));

        registry.put(ClassSpec.DRUID_NONE, weights(Map.of(
                StatType.STRENGTH, 1.0,
                StatType.STAMINA, 0.6,
                StatType.INTELLECT, 0.5,
                StatType.SPIRIT, 0.4,
                StatType.ARMOR, 0.01
        )));

        // ==========================================
        // WARRIOR
        // ==========================================
        registry.put(ClassSpec.WARRIOR_ARMS, weights(Map.of(
                StatType.STRENGTH, 2.0,
                StatType.ATTACK_POWER, 1.0,
                StatType.AGILITY, 1.0,
                StatType.CRIT_RATING, 25.0,
                StatType.HIT_RATING, 21.0,
                StatType.STAMINA, 0.2,
                StatType.ARMOR, 0.01,
                StatType.WEAPON_DPS, 14.0,
                StatType.WEAPON_SPEED, 25.0
        )));

        registry.put(ClassSpec.WARRIOR_FURY, weights(Map.of(
                StatType.STRENGTH, 2.0,
                StatType.ATTACK_POWER, 1.0,
                StatType.AGILITY, 1.0,
                StatType.CRIT_RATING, 25.0,
                StatType.HIT_RATING, 21.0,
                StatType.STAMINA, 0.1,
                StatType.ARMOR, 0.01,
                StatType.WEAPON_DPS, 16.5,
                StatType.WEAPON_SPEED, 5.0
        )));

        registry.put(ClassSpec.WARRIOR_PROTECTION, weights(Map.ofEntries(
                entry(StatType.STAMINA, 1.5),
                entry(StatType.DEFENSE_RATING, 2.0),
                entry(StatType.STRENGTH, 1.2),
                entry(StatType.ATTACK_POWER, 0.5),
                entry(StatType.DODGE_RATING, 1.2),
                entry(StatType.PARRY_RATING, 1.4),
                entry(StatType.BLOCK_CHANCE, 1.0),
                entry(StatType.BLOCK_VALUE, 1.5),
                entry(StatType.AGILITY, 0.8),
                entry(StatType.ARMOR, 0.04),
                entry(StatType.HIT_RATING, 15.0),
                entry(StatType.CRIT_RATING, 10.0),
                entry(StatType.WEAPON_DPS, 9.0),
                entry(StatType.WEAPON_SPEED, -5.0)
        )));

        // ==========================================
        // ROGUE
        // ==========================================
        registry.put(ClassSpec.ROGUE_ASSASSINATION, weights(Map.of(
                StatType.AGILITY, 1.6,
                StatType.ATTACK_POWER, 1.0,
                StatType.STRENGTH, 0.6,
                StatType.ARMOR, 0.01,
                StatType.CRIT_RATING, 23.0,
                StatType.HIT_RATING, 19.0,
                StatType.WEAPON_DPS, 12.0,
                StatType.WEAPON_SPEED, -10.0
        )));

        registry.put(ClassSpec.ROGUE_COMBAT, weights(Map.of(
                StatType.AGILITY, 1.5,
                StatType.ATTACK_POWER, 1.0,
                StatType.STRENGTH, 1.0,
                StatType.ARMOR, 0.01,
                StatType.CRIT_RATING, 24.0,
                StatType.HIT_RATING, 18.0,
                StatType.WEAPON_DPS, 15.0,
                StatType.WEAPON_SPEED, 12.0
        )));

        registry.put(ClassSpec.ROGUE_SUBTLETY, weights(Map.of(
                StatType.AGILITY, 1.4,
                StatType.ATTACK_POWER, 1.0,
                StatType.STRENGTH, 0.8,
                StatType.ARMOR, 0.01,
                StatType.CRIT_RATING, 22.0,
                StatType.HIT_RATING, 17.0,
                StatType.WEAPON_DPS, 11.0,
                StatType.WEAPON_SPEED, 15.0
        )));

        // ==========================================
        // PALADIN
        // ==========================================
        registry.put(ClassSpec.PALADIN_HOLY, weights(Map.of(
                StatType.HEALING_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.SPELL_POWER, 0.85,
                StatType.MANA_PER_5_SECONDS, 5.0,
                StatType.SPELL_CRIT_RATING, 7.0,
                StatType.SPIRIT, 0.1,
                StatType.STAMINA, 0.2,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.PALADIN_PROTECTION, weights(Map.ofEntries(
                entry(StatType.STAMINA, 1.5),
                entry(StatType.SPELL_POWER, 1.2),
                entry(StatType.DEFENSE_RATING, 1.5),
                entry(StatType.DODGE_RATING, 1.0),
                entry(StatType.PARRY_RATING, 1.2),
                entry(StatType.BLOCK_CHANCE, 1.0),
                entry(StatType.BLOCK_VALUE, 1.0),
                entry(StatType.STRENGTH, 1.0),
                entry(StatType.INTELLECT, 0.5),
                entry(StatType.MANA_PER_5_SECONDS, 2.0),
                entry(StatType.ARMOR, 0.04),
                entry(StatType.HIT_RATING, 12.0),
                entry(StatType.WEAPON_DPS, 6.0)
        )));

        registry.put(ClassSpec.PALADIN_RETRIBUTION, weights(Map.ofEntries(
                entry(StatType.STRENGTH, 2.0),
                entry(StatType.ARMOR, 0.01),
                entry(StatType.ATTACK_POWER, 1.0),
                entry(StatType.SPELL_POWER, 0.6),
                entry(StatType.AGILITY, 1.0),
                entry(StatType.CRIT_RATING, 22.0),
                entry(StatType.HIT_RATING, 20.0),
                entry(StatType.INTELLECT, 0.5),
                entry(StatType.MANA_PER_5_SECONDS, 1.5),
                entry(StatType.WEAPON_DPS, 11.0),
                entry(StatType.WEAPON_SPEED, 35.0)
        )));

        // ==========================================
        // HUNTER
        // ==========================================
        registry.put(ClassSpec.HUNTER_BEASTMASTERY, weights(Map.of(
                StatType.RANGED_ATTACK_POWER, 1.0,
                StatType.AGILITY, 2.0,
                StatType.CRIT_RATING, 24.0,
                StatType.HIT_RATING, 22.0,
                StatType.MANA_PER_5_SECONDS, 1.0,
                StatType.STAMINA, 0.2,
                StatType.ARMOR, 0.01,
                StatType.WEAPON_DPS, 20.0,
                StatType.WEAPON_SPEED, 5.0
        )));

        registry.put(ClassSpec.HUNTER_MARKSMANSHIP, weights(Map.of(
                StatType.RANGED_ATTACK_POWER, 1.0,
                StatType.AGILITY, 2.5,
                StatType.CRIT_RATING, 26.0,
                StatType.HIT_RATING, 24.0,
                StatType.MANA_PER_5_SECONDS, 1.0,
                StatType.STAMINA, 0.2,
                StatType.ARMOR, 0.01,
                StatType.WEAPON_DPS, 28.0,
                StatType.WEAPON_SPEED, 18.0
        )));

        registry.put(ClassSpec.HUNTER_SURVIVAL, weights(Map.of(
                StatType.RANGED_ATTACK_POWER, 1.0,
                StatType.AGILITY, 2.2,
                StatType.CRIT_RATING, 25.0,
                StatType.HIT_RATING, 23.0,
                StatType.MANA_PER_5_SECONDS, 1.0,
                StatType.STAMINA, 0.4,
                StatType.ARMOR, 0.01,
                StatType.WEAPON_DPS, 22.0,
                StatType.WEAPON_SPEED, 10.0
        )));

        // ==========================================
        // PRIEST
        // ==========================================
        registry.put(ClassSpec.PRIEST_DISCIPLINE, weights(Map.of(
                StatType.HEALING_POWER, 1.0,
                StatType.SPELL_POWER, 0.85,
                StatType.INTELLECT, 1.0,
                StatType.SPIRIT, 0.9,
                StatType.MANA_PER_5_SECONDS, 4.0,
                StatType.SPELL_CRIT_RATING, 6.0,
                StatType.STAMINA, 0.3,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.PRIEST_HOLY, weights(Map.of(
                StatType.HEALING_POWER, 1.0,
                StatType.SPELL_POWER, 0.85,
                StatType.INTELLECT, 1.0,
                StatType.SPIRIT, 0.8,
                StatType.MANA_PER_5_SECONDS, 4.5,
                StatType.SPELL_CRIT_RATING, 6.0,
                StatType.STAMINA, 0.2,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.PRIEST_SHADOW, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.SPELL_HIT_RATING, 15.0,
                StatType.SPELL_CRIT_RATING, 8.0,
                StatType.MANA_PER_5_SECONDS, 2.0,
                StatType.SPIRIT, 0.4,
                StatType.STAMINA, 0.3,
                StatType.ARMOR, 0.001
        )));

        // ==========================================
        // SHAMAN
        // ==========================================
        registry.put(ClassSpec.SHAMAN_ELEMENTAL, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.SPELL_CRIT_RATING, 9.0,
                StatType.SPELL_HIT_RATING, 13.0,
                StatType.MANA_PER_5_SECONDS, 3.0,
                StatType.SPIRIT, 0.1,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.SHAMAN_ENHANCEMENT, weights(Map.of(
                StatType.STRENGTH, 2.0,
                StatType.ATTACK_POWER, 1.0,
                StatType.AGILITY, 1.0,
                StatType.ARMOR, 0.01,
                StatType.CRIT_RATING, 22.0,
                StatType.HIT_RATING, 20.0,
                StatType.INTELLECT, 0.4,
                StatType.MANA_PER_5_SECONDS, 2.0,
                StatType.WEAPON_DPS, 12.5,
                StatType.WEAPON_SPEED, 32.0
        )));

        registry.put(ClassSpec.SHAMAN_RESTORATION, weights(Map.of(
                StatType.HEALING_POWER, 1.0,
                StatType.SPELL_POWER, 0.85,
                StatType.INTELLECT, 1.0,
                StatType.MANA_PER_5_SECONDS, 4.5,
                StatType.SPIRIT, 0.1,
                StatType.SPELL_CRIT_RATING, 6.0,
                StatType.STAMINA, 0.3,
                StatType.ARMOR, 0.001
        )));

        // ==========================================
        // MAGE
        // ==========================================
        registry.put(ClassSpec.MAGE_ARCANE, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.2,
                StatType.SPELL_CRIT_RATING, 10.0,
                StatType.SPELL_HIT_RATING, 14.0,
                StatType.MANA_PER_5_SECONDS, 2.0,
                StatType.SPIRIT, 0.3,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.MAGE_FIRE, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.SPELL_CRIT_RATING, 12.0,
                StatType.SPELL_HIT_RATING, 15.0,
                StatType.MANA_PER_5_SECONDS, 2.0,
                StatType.SPIRIT, 0.2,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.MAGE_FROST, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.SPELL_CRIT_RATING, 11.0,
                StatType.SPELL_HIT_RATING, 14.0,
                StatType.MANA_PER_5_SECONDS, 2.0,
                StatType.SPIRIT, 0.2,
                StatType.ARMOR, 0.001
        )));

        // ==========================================
        // WARLOCK
        // ==========================================
        registry.put(ClassSpec.WARLOCK_AFFLICTION, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.SPELL_HIT_RATING, 14.0,
                StatType.SPELL_CRIT_RATING, 8.0,
                StatType.STAMINA, 0.5,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.WARLOCK_DEMONOLOGY, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.STAMINA, 0.8,
                StatType.ARMOR, 0.001,
                StatType.SPELL_CRIT_RATING, 8.0,
                StatType.SPELL_HIT_RATING, 12.0
        )));

        registry.put(ClassSpec.WARLOCK_DESTRUCTION, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.SPELL_CRIT_RATING, 11.0,
                StatType.SPELL_HIT_RATING, 15.0,
                StatType.STAMINA, 0.4,
                StatType.ARMOR, 0.001
        )));

        // ==========================================
        // DRUID
        // ==========================================
        registry.put(ClassSpec.DRUID_BALANCE, weights(Map.of(
                StatType.SPELL_POWER, 1.0,
                StatType.INTELLECT, 1.0,
                StatType.SPELL_CRIT_RATING, 10.0,
                StatType.SPELL_HIT_RATING, 14.0,
                StatType.MANA_PER_5_SECONDS, 3.0,
                StatType.SPIRIT, 0.5,
                StatType.ARMOR, 0.001
        )));

        registry.put(ClassSpec.DRUID_FERAL, weights(Map.of(
                StatType.AGILITY, 1.4,
                StatType.STRENGTH, 1.2,
                StatType.ATTACK_POWER, 0.6,
                StatType.CRIT_RATING, 23.0,
                StatType.HIT_RATING, 20.0,
                StatType.STAMINA, 0.8,
                StatType.DEFENSE_RATING, 1.2,
                StatType.DODGE_RATING, 1.0,
                StatType.ARMOR, 0.12
        )));

        registry.put(ClassSpec.DRUID_RESTORATION, weights(Map.of(
                StatType.HEALING_POWER, 1.0,
                StatType.SPELL_POWER, 0.85,
                StatType.INTELLECT, 1.0,
                StatType.SPIRIT, 0.9,
                StatType.MANA_PER_5_SECONDS, 3.0,
                StatType.SPELL_CRIT_RATING, 5.0,
                StatType.STAMINA, 0.3,
                StatType.ARMOR, 0.001
        )));
    }

    private static StatWeights weights(Map<StatType, Double> map) {
        return new StatWeights(new EnumMap<>(map));
    }

}
