package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse.ItemDTO.*;

public class EquipmentResponseBuilder {

    public record Suffix(long id, String name) {
    }

    private Long id;
    private String fullName;
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

    private Map<Long, String> enchantments;

    private EquipmentResponseBuilder() {
    }

    public static EquipmentResponseBuilder newInstance(ItemResponse base) {
        EquipmentResponseBuilder builder = new EquipmentResponseBuilder();
        builder.id = base.id();
        builder.fullName = base.name();
        builder.quality = base.quality() != null ? base.quality().type() : null;
        builder.itemLevel = base.itemLevel() != null ? base.itemLevel() : null;
        builder.stats = mapStatsFromBase(base);
        builder.armorValue = mapArmorFromBase(base);
        mapWeaponFromBase(builder, base);
        return builder;
    }

    private static Map<String, Integer> mapStatsFromBase(ItemResponse base) {
        if (base.preview() == null || base.preview().stats() == null) return null;
        return base.preview().stats().stream()
                .collect(Collectors.toMap(
                        (stat) -> stat.type().type(),
                        ItemResponse.StatDTO::value
                ));
    }

    private static Integer mapArmorFromBase(ItemResponse base) {
        if (base.preview() == null || base.preview().armor() == null) return null;
        return base.preview().armor().value();
    }

    private static void mapWeaponFromBase(EquipmentResponseBuilder builder, ItemResponse base) {
        if (base.preview() != null && base.preview().weapon() != null) {
            builder.attackSpeed = base.preview().weapon().attackSpeed().value();
            builder.dps = base.preview().weapon().dps().value();
            builder.minDamage = base.preview().weapon().damage().minValue();
            builder.maxDamage = base.preview().weapon().damage().maxValue();
        }
    }

    public EquipmentResponse.ItemDTO build() {
        return new EquipmentResponse.ItemDTO(
                this.id != null ? new ItemDTOReference(id) : null,
                new SlotDTO(this.slot),
                fullName(),
                new QualityDTO(this.quality),
                this.itemLevel != null ? new LevelDTO(this.itemLevel) : null,
                stats(),
                this.armorValue != null ? new ArmorDTO(this.armorValue) : null,
                weaponDTO(),
                enchantments()
        );
    }

    private String fullName() {
        if (fullName == null) return null;
        return this.suffix != null ? this.fullName + " " + this.suffix.name() : this.fullName;
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

    public EquipmentResponseBuilder withFullName(String fullName) {
        this.fullName = fullName;
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

    public EquipmentResponseBuilder withEnchantments(Map<Long, String> enchs) {
        if (enchs != null) {
            if (this.enchantments == null) this.enchantments = new HashMap<>();
            this.enchantments.putAll(enchs);
        }
        return this;
    }
}
