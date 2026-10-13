package com.bebis.BeBiS.item;

import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.tools.EnumTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeaponBuilder {

    private Long baseId;
    private Long suffixId;
    private String name;
    private String inventoryType;
    private String quality;
    private int itemLevel;
    private int requiredLevel;
    private boolean uniqueEquipped;

    private Map<String, Integer> stats;
    private List<String> specialEffects;

    // weapon-specific stuff
    private double speed;
    private int minDamage;
    private int maxDamage;
    private double dps;
    private String weaponType;

    private WeaponBuilder() {
    }

    public static WeaponBuilder newOneHandSword() {
        WeaponBuilder builder = new WeaponBuilder();
        builder.baseId = 6802L;
        builder.suffixId = 0L;
        builder.name = "Sword of Omen";
        builder.inventoryType = "WEAPON";
        builder.quality = "RARE";
        builder.itemLevel = 44;
        builder.requiredLevel = 0;
        builder.uniqueEquipped = false;
        builder.stats = Map.of("STRENGTH", 9, "AGILITY", 3, "STAMINA", 4);
        builder.speed = 1.90;
        builder.minDamage = 39;
        builder.maxDamage = 74;
        builder.dps = 29.74;
        builder.weaponType = "SWORD";
        return builder;
    }

    public static WeaponBuilder newRanged() {
        WeaponBuilder builder = new WeaponBuilder();
        builder.baseId = 18713L;
        builder.suffixId = 0L;
        builder.name = "Rhok'delar, Longbow of the Ancient Keepers";
        builder.inventoryType = "RANGED";
        builder.quality = "EPIC";
        builder.itemLevel = 75;
        builder.requiredLevel = 60;
        builder.stats = Map.of("CRIT_RATING", 1, "RANGED_ATTACK_POWER", 17);
        builder.speed = 2.90;
        builder.minDamage = 89;
        builder.maxDamage = 166;
        builder.dps = 43.97;
        builder.weaponType = "BOW";
        return builder;
    }

    public WeaponBuilder withBaseId(long baseId) {
        this.baseId = baseId;
        return this;
    }

    public WeaponBuilder withSuffixId(long suffixId) {
        this.suffixId = suffixId;
        return this;
    }

    public WeaponBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public WeaponBuilder withInventoryType(String inventoryType) {
        this.inventoryType = inventoryType;
        return this;
    }

    public WeaponBuilder withQuality(String quality) {
        this.quality = quality;
        return this;
    }

    public WeaponBuilder withItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
        return this;
    }

    public WeaponBuilder withRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
        return this;
    }

    public WeaponBuilder withUniqueEquipped(boolean uniqueEquipped) {
        this.uniqueEquipped = uniqueEquipped;
        return this;
    }

    public WeaponBuilder withStats(Map<String, Integer> stats) {
        this.stats = stats;
        return this;
    }

    public WeaponBuilder withSpecialEffects(List<String> specialEffects) {
        this.specialEffects = specialEffects;
        return this;
    }

    public WeaponBuilder withSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public WeaponBuilder withMinDamage(int minDamage) {
        this.minDamage = minDamage;
        return this;
    }

    public WeaponBuilder withMaxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
        return this;
    }

    public WeaponBuilder withDps(double dps) {
        this.dps = dps;
        return this;
    }

    public WeaponBuilder withWeaponType(String wType) {
        this.weaponType = wType;
        return this;
    }

    public Weapon build() {
        Item.ItemMetadata metadata = new Item.ItemMetadata(
                itemKey(),
                name(),
                inventoryType(),
                quality(),
                this.itemLevel,
                this.requiredLevel,
                this.uniqueEquipped,
                stats(),
                this.specialEffects
        );
        return new Weapon(metadata, this.speed, this.minDamage, this.maxDamage, this.dps, weaponType());
    }

    private Item.ItemKey itemKey() {
        return new Item.ItemKey(this.baseId, this.suffixId);
    }

    private String name() {
        if (this.name == null || this.name.isEmpty()) {
            throw new IllegalArgumentException("Either null or empty name");
        }
        return this.name;
    }

    private Item.InventoryType inventoryType() {
        try {
            return Item.InventoryType.valueOf(this.inventoryType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid inventory type: " + this.inventoryType);
        }
    }

    private Item.Quality quality() {
        try {
            return Item.Quality.valueOf(this.quality);
        } catch (Exception e) {
            return Item.Quality.UNKNOWN;
        }
    }

    private Map<StatType, Integer> stats() {
        Map<StatType, Integer> statsMap = new HashMap<>();
        this.stats.forEach((statType, value) ->
                EnumTools.fromString(StatType.class, statType).ifPresent((stat) -> statsMap.put(stat, value)));
        return statsMap;
    }

    private Weapon.WeaponType weaponType() {
        try {
            return Weapon.WeaponType.valueOf(this.weaponType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid weapon type: " + this.weaponType);
        }
    }
}
