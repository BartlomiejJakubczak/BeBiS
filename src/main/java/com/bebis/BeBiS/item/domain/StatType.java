package com.bebis.BeBiS.item.domain;

import java.util.Optional;

public enum StatType {
    STRENGTH,
    AGILITY,
    INTELLECT,
    STAMINA,
    SPIRIT,
    SPELL_CRIT_RATING,
    CRIT_RATING,
    MANA_PER_5_SECONDS,
    HIT_RATING,
    SPELL_HIT_RATING,
    ATTACK_POWER,
    RANGED_ATTACK_POWER,
    SPELL_POWER,
    HEALING_POWER,
    DEFENSE_RATING,
    DODGE_RATING,
    PARRY_RATING,
    BLOCK_CHANCE,
    BLOCK_VALUE,
    ARMOR,
    WEAPON_DPS,
    WEAPON_SPEED;

    public static Optional<StatType> fromString(String type) {
        try {
            return Optional.of(StatType.valueOf(type.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
