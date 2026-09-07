package com.bebis.BeBiS.item;

import com.bebis.BeBiS.item.domain.EquippableItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.tools.EnumTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemBuilder {

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

    private ItemBuilder() {

    }

    public static ItemBuilder newEquippableItem() {
        ItemBuilder builder = new ItemBuilder();
        builder.baseId = 17063L;
        builder.suffixId = 0L;
        builder.name = "Band of Accuria";
        builder.inventoryType = "FINGER";
        builder.quality = "EPIC";
        builder.itemLevel = 78;
        builder.requiredLevel = 60;
        builder.uniqueEquipped = false;
        builder.stats = Map.of("AGILITY", 16, "STAMINA", 10, "HIT_RATING", 2);
        return builder;
    }

    public ItemBuilder withBaseId(long baseId) {
        this.baseId = baseId;
        return this;
    }

    public ItemBuilder withSuffixId(long suffixId) {
        this.suffixId = suffixId;
        return this;
    }

    public ItemBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder withInventoryType(String inventoryType) {
        this.inventoryType = inventoryType;
        return this;
    }

    public ItemBuilder withQuality(String quality) {
        this.quality = quality;
        return this;
    }

    public ItemBuilder withItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
        return this;
    }

    public ItemBuilder withRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
        return this;
    }

    public ItemBuilder withUniqueEquipped(boolean uniqueEquipped) {
        this.uniqueEquipped = uniqueEquipped;
        return this;
    }

    public ItemBuilder withStats(Map<String, Integer> stats) {
        this.stats = stats;
        return this;
    }

    public ItemBuilder withSpecialEffects(List<String> specialEffects) {
        this.specialEffects = specialEffects;
        return this;
    }

    public EquippableItem build() {
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
        return new EquippableItem(metadata);
    }

    private Item.ItemKey itemKey() {
        if (this.baseId == null || this.suffixId == null) {
            throw new IllegalArgumentException("Either baseId or suffixId is null");
        }
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
}
