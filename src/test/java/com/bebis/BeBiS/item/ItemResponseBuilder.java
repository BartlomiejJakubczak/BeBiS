package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;

import java.util.List;
import java.util.Map;

import static com.bebis.BeBiS.integration.blizzard.dto.ItemResponse.*;

public class ItemResponseBuilder {

    public record ItemClass(int id, String type) {
    }

    public record SubClass(int id, String type) {
    }

    private Long id;
    private String name;
    private String quality;
    private Integer itemLevel;
    private Integer requiredLevel;
    private ItemClass itemClass;
    private SubClass subClass;
    private String inventoryType;
    private Boolean uniqueEquipped;

    private Map<String, Integer> stats;
    private List<String> spells;

    private Integer armorValue;

    private Double attackSpeed;
    private Double dps;
    private Integer minDamage;
    private Integer maxDamage;

    private ItemResponseBuilder() {
    }

    public static ItemResponseBuilder newWeaponInstance() {
        ItemResponseBuilder builder = new ItemResponseBuilder();
        builder.id = 6975L;
        builder.name = "Whirlwind Axe";
        builder.quality = "RARE";
        builder.itemLevel = 40;
        builder.requiredLevel = 30;
        builder.itemClass = new ItemClass(2, "Weapon"); // weapon class
        builder.subClass = new SubClass(1, "Axe"); // 2h axe subclass
        builder.inventoryType = "TWO_HAND";
        builder.stats = Map.of("STRENGTH", 15, "STAMINA", 14);
        builder.attackSpeed = 3.6;
        builder.minDamage = 82;
        builder.maxDamage = 124;
        builder.dps = 28.61;
        return builder;
    }

    public static ItemResponseBuilder newSuffixableWeaponInstance() {
        ItemResponseBuilder builder = new ItemResponseBuilder();
        builder.id = 13018L;
        builder.name = "Gigantic Axe";
        builder.quality = "UNCOMMON";
        builder.itemLevel = 43;
        builder.requiredLevel = 38;
        builder.itemClass = new ItemClass(2, "Weapon"); // weapon class
        builder.subClass = new SubClass(1, "Axe"); // 2h axe subclass
        builder.inventoryType = "TWO_HAND";
        builder.attackSpeed = 3.4;
        builder.minDamage = 75;
        builder.maxDamage = 113;
        builder.dps = 27.65;
        return builder;
    }

    public static ItemResponseBuilder newArmorInstance() {
        ItemResponseBuilder builder = new ItemResponseBuilder();
        builder.id = 10328L;
        builder.name = "Scarlet Chestpiece";
        builder.quality = "RARE";
        builder.itemLevel = 43;
        builder.requiredLevel = 34;
        builder.itemClass = new ItemClass(4, "Armor");
        builder.subClass = new SubClass(3, "Mail");
        builder.inventoryType = "CHEST";
        builder.stats = Map.of("STRENGTH", 8, "STAMINA", 19);
        builder.armorValue = 250;
        return builder;
    }

    public static ItemResponseBuilder newSuffixableArmorInstance() {
        ItemResponseBuilder builder = new ItemResponseBuilder();
        builder.id = 3829L;
        builder.name = "Brigandine Chestpiece";
        builder.quality = "UNCOMMON";
        builder.itemLevel = 32;
        builder.requiredLevel = 27;
        builder.itemClass = new ItemClass(4, "Armor");
        builder.subClass = new SubClass(3, "Mail");
        builder.inventoryType = "CHEST";
        builder.armorValue = 231;
        return builder;
    }

    public static ItemResponseBuilder newEquippableInstance() {
        ItemResponseBuilder builder = new ItemResponseBuilder();
        builder.id = 18500L;
        builder.name = "Tarnished Elven Ring";
        builder.quality = "RARE";
        builder.itemLevel = 59;
        builder.requiredLevel = 54;
        builder.itemClass = new ItemClass(4, "Armor");
        builder.subClass = new SubClass(0, "Misc");
        builder.inventoryType = "FINGER";
        builder.stats = Map.of("AGILITY", 15);
        return builder;
    }

    public static ItemResponseBuilder newSuffixableEquippableInstance() {
        ItemResponseBuilder builder = new ItemResponseBuilder();
        builder.id = 12022L;
        builder.name = "Gold Ring";
        builder.quality = "UNCOMMON";
        builder.itemLevel = 40;
        builder.requiredLevel = 35;
        builder.itemClass = new ItemClass(4, "Armor");
        builder.subClass = new SubClass(0, "Misc");
        builder.inventoryType = "FINGER";
        return builder;
    }

    public ItemResponse build() {
        return new ItemResponse(
                this.id,
                this.name,
                new QualityDTO(this.quality),
                this.requiredLevel,
                this.itemLevel,
                itemClassDTO(),
                subClassDTO(),
                new InventoryTypeDTO(this.inventoryType),
                previewItemDTO()
        );
    }

    private ItemClassDTO itemClassDTO() {
        return this.itemClass != null ? new ItemClassDTO(this.itemClass.id(), this.itemClass.type()) : null;
    }

    private SubclassDTO subClassDTO() {
        return this.subClass != null ? new SubclassDTO(this.subClass.id(), this.subClass.type()) : null;
    }

    private PreviewItemDTO previewItemDTO() {
        return new PreviewItemDTO(statsDTO(), weaponDTO(), armorDTO(), spellEffectDTO(), uniqueEquipped());
    }

    private List<StatDTO> statsDTO() {
        if (this.stats == null) return null;
        return stats.entrySet().stream()
                .map((entry) -> new StatDTO(new StatDTO.StatTypeWrapper(entry.getKey()), entry.getValue()))
                .toList();
    }

    private WeaponDTO weaponDTO() {
        if (this.minDamage == null && this.maxDamage == null && this.dps == null && this.attackSpeed == null)
            return null;
        return new WeaponDTO(new WeaponDTO.AttackSpeedDTO(this.attackSpeed),
                new WeaponDTO.DamageDTO(this.minDamage, this.maxDamage),
                new WeaponDTO.DpsDTO(this.dps));
    }

    private ArmorDTO armorDTO() {
        if (this.armorValue == null) return null;
        return new ArmorDTO(this.armorValue);
    }

    private List<PreviewItemDTO.SpellEffectDTO> spellEffectDTO() {
        if (this.spells == null) return null;
        return spells.stream().map(PreviewItemDTO.SpellEffectDTO::new).toList();
    }

    private String uniqueEquipped() {
        return this.uniqueEquipped != null && this.uniqueEquipped ? "Whatever string that they put in to indicate uniqueness" : null;
    }

    public ItemResponseBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ItemResponseBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ItemResponseBuilder withQuality(String quality) {
        this.quality = quality;
        return this;
    }

    public ItemResponseBuilder withItemLevel(Integer itemLevel) {
        this.itemLevel = itemLevel;
        return this;
    }

    public ItemResponseBuilder withRequiredLevel(Integer requiredLevel) {
        this.requiredLevel = requiredLevel;
        return this;
    }

    public ItemResponseBuilder withItemClass(ItemClass itemClass) {
        this.itemClass = itemClass;
        return this;
    }

    public ItemResponseBuilder withSubClass(SubClass subClass) {
        this.subClass = subClass;
        return this;
    }

    public ItemResponseBuilder withInventoryType(String inventoryType) {
        this.inventoryType = inventoryType;
        return this;
    }

    public ItemResponseBuilder withUniqueEquipped(Boolean uniqueEquipped) {
        this.uniqueEquipped = uniqueEquipped;
        return this;
    }

    public ItemResponseBuilder withArmorValue(Integer armorValue) {
        this.armorValue = armorValue;
        return this;
    }

    public ItemResponseBuilder withSpells(List<String> spells) {
        this.spells = spells != null ? List.copyOf(spells) : null;
        return this;
    }

    public ItemResponseBuilder withStats(Map<String, Integer> stats) {
        this.stats = stats != null ? Map.copyOf(stats) : null;
        return this;
    }

    public ItemResponseBuilder withAttackSpeed(Double speed) {
        this.attackSpeed = speed;
        return this;
    }

    public ItemResponseBuilder withMinDamage(Integer minDamage) {
        this.minDamage = minDamage;
        return this;
    }

    public ItemResponseBuilder withMaxDamage(Integer maxDamage) {
        this.maxDamage = maxDamage;
        return this;
    }

    public ItemResponseBuilder withDps(Double dps) {
        this.dps = dps;
        return this;
    }

}
