package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse.ItemDTO.*;

public class EquipmentResponseBuilder {

    public record Suffix(int id, String name) {
    }

    private ItemResponse base;

    private Long id;
    private String name;
    private Suffix suffix;
    private String slot;
    private String quality;
    private Integer itemLevel;

    private Map<String, Integer> stats;

    private Integer armorValue;

    private Double attackSpeed;
    private Double dps;
    private Integer minDamage;
    private Integer maxDamage;

    private Map<Integer, String> enchantments;

    private EquipmentResponseBuilder(ItemResponse base) {
        this.base = base;
    }

    public static EquipmentResponseBuilder newInstance(ItemResponse item) {
        return new EquipmentResponseBuilder(item);
    }

    public EquipmentResponse.ItemDTO build() {
        return new EquipmentResponse.ItemDTO(
                this.id != null ? new ItemDTOReference(id) : new ItemDTOReference(base.id()),
                new SlotDTO(this.slot),
                fullName(),
                new QualityDTO(this.quality),
                new LevelDTO(this.itemLevel),
                stats(),
                new ArmorDTO(this.armorValue),
                weaponDTO(),
                enchantments()
        );
    }

    private String fullName() {
        if (name == null) {
            return this.suffix != null ? this.base.name() + " " + this.suffix.name() : this.base.name();
        }
        return name;
    }

    private List<StatDTO> stats() {
        if (this.stats == null) return null;
        return stats.entrySet().stream()
                .map(entry -> new StatDTO(new StatDTO.StatTypeWrapper(entry.getKey()), entry.getValue()))
                .toList();
    }

    private WeaponDTO weaponDTO() {
        if (this.minDamage == null && this.maxDamage == null && this.dps == null && this.attackSpeed == null)
            return null;
        return new WeaponDTO(new WeaponDTO.DamageDTO(this.minDamage, this.maxDamage),
                new WeaponDTO.AttackSpeedDTO(this.attackSpeed),
                new WeaponDTO.DpsDTO(this.dps));
    }

    private List<EnchantmentDTO> enchantments() {
        if (this.enchantments == null) return null;
        return this.enchantments.entrySet().stream()
                .map(entry -> new EnchantmentDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    public EquipmentResponseBuilder withSuffix(Suffix suffix) {
        this.suffix = suffix;
        if (this.enchantments == null) this.enchantments = new HashMap<>();
        this.enchantments.put(suffix.id(), suffix.name());
        return this;
    }

    private EquipmentResponseBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public EquipmentResponseBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public EquipmentResponseBuilder withSlot(String slot) {
        this.slot = slot;
        return this;
    }

    public EquipmentResponseBuilder withQuality(String quality) {
        this.quality = quality;
        return this;
    }

    public EquipmentResponseBuilder withItemLevel(Integer itemLevel) {
        this.itemLevel = itemLevel;
        return this;
    }

    public EquipmentResponseBuilder withStats(Map<String, Integer> stats) {
        this.stats = stats != null ? Map.copyOf(stats) : null;
        return this;
    }

    public EquipmentResponseBuilder withArmorValue(Integer armorValue) {
        this.armorValue = armorValue;
        return this;
    }

    public EquipmentResponseBuilder withAttackSpeed(Double speed) {
        this.attackSpeed = speed;
        return this;
    }

    public EquipmentResponseBuilder withMinDamage(Integer minDamage) {
        this.minDamage = minDamage;
        return this;
    }

    public EquipmentResponseBuilder withMaxDamage(Integer maxDamage) {
        this.maxDamage = maxDamage;
        return this;
    }

    public EquipmentResponseBuilder withDps(Double dps) {
        this.dps = dps;
        return this;
    }

    public EquipmentResponseBuilder withEnchantments(Map<Integer, String> enchs) {
        if (enchs != null) {
            if (this.enchantments == null) this.enchantments = new HashMap<>();
            this.enchantments.putAll(enchs);
        }
        return this;
    }
}
