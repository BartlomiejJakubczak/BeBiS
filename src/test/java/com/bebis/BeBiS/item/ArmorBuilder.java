package com.bebis.BeBiS.item;

import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.tools.EnumTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArmorBuilder {

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

    // armor-specific stuff
    private String armorType;

    private ArmorBuilder() {
    }

    public static ArmorBuilder newShield() {
        ArmorBuilder builder = new ArmorBuilder();
        builder.baseId = 23043L;
        builder.suffixId = 0L;
        builder.name = "The Face of Death";
        builder.inventoryType = "SHIELD";
        builder.quality = "EPIC";
        builder.itemLevel = 90;
        builder.requiredLevel = 60;
        builder.uniqueEquipped = false;
        builder.stats = Map.of("ARMOR", 3494, "BLOCK_VALUE", 87, "STAMINA", 21, "BLOCK_CHANCE", 3);
        builder.armorType = "SHIELD";
        return builder;
    }

    public ArmorBuilder withBaseId(long baseId) {
        this.baseId = baseId;
        return this;
    }

    public ArmorBuilder withSuffixId(long suffixId) {
        this.suffixId = suffixId;
        return this;
    }

    public ArmorBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ArmorBuilder withInventoryType(String inventoryType) {
        this.inventoryType = inventoryType;
        return this;
    }

    public ArmorBuilder withQuality(String quality) {
        this.quality = quality;
        return this;
    }

    public ArmorBuilder withItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
        return this;
    }

    public ArmorBuilder withRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
        return this;
    }

    public ArmorBuilder withUniqueEquipped(boolean uniqueEquipped) {
        this.uniqueEquipped = uniqueEquipped;
        return this;
    }

    public ArmorBuilder withStats(Map<String, Integer> stats) {
        this.stats = stats;
        return this;
    }

    public ArmorBuilder withSpecialEffects(List<String> specialEffects) {
        this.specialEffects = specialEffects;
        return this;
    }

    public ArmorBuilder withArmorType(String aType) {
        this.armorType = aType;
        return this;
    }

    public Armor build() {
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
        return new Armor(metadata, armorType());
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

    private Armor.ArmorType armorType() {
        try {
            return Armor.ArmorType.valueOf(this.armorType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid weapon type: " + this.armorType);
        }
    }
}
