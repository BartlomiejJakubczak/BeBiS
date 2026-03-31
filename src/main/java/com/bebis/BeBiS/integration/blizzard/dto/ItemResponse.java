package com.bebis.BeBiS.integration.blizzard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A Domain-level representation of a WoW Item.
 * Note: @JsonIgnoreProperties(ignoreUnknown = true) ensures we safely ignore
 * any Blizzard bloat we don't care about.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemResponse(
        long id,
        String name,
        QualityDTO quality,
        @JsonProperty("level") int itemLevel,
        @JsonProperty("required_level") int requiredLevel,
        @JsonProperty("item_class") ItemClassDTO itemClass, // Defines Weapon/Armor/Misc
        @JsonProperty("item_subclass") SubclassDTO subclass, // Defines Sword/Plate/etc
        @JsonProperty("inventory_type") InventoryTypeDTO inventoryType,
        @JsonProperty("preview_item") PreviewItemDTO preview // Holds stats, weapon damage, armor, and effects
) {
    public record ItemClassDTO(int id, String name) {
    }

    public record SubclassDTO(long id, String name) {
    }

    public record QualityDTO(String type) {
    }

    public record InventoryTypeDTO(String type) {
    }

    public record PreviewItemDTO(
            List<StatDTO> stats,
            WeaponDTO weapon,
            ArmorDTO armor,
            @JsonProperty("spells") List<SpellEffectDTO> spells,
            @JsonProperty("unique_equipped") String uniqueEquipped
    ) {
        public record SpellEffectDTO(String description) {
        }
    }

    public record StatDTO(StatTypeWrapper type, int value) {
        public record StatTypeWrapper(String type) {
        }
    }

    public record WeaponDTO(
            @JsonProperty("attack_speed") AttackSpeedDTO attackSpeed,
            DamageDTO damage,
            DpsDTO dps
    ) {

        public record DamageDTO(
                @JsonProperty("min_value") int minValue,
                @JsonProperty("max_value") int maxValue
        ) {
        }

        public record AttackSpeedDTO(
                double value
        ) {
        }

        public record DpsDTO(
                double value
        ) {
        }
    }

    public record ArmorDTO(int value) {
    }
}