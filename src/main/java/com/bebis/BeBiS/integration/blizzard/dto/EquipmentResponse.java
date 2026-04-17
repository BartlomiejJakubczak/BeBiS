package com.bebis.BeBiS.integration.blizzard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EquipmentResponse(
        @JsonProperty("equipped_items") List<ItemDTO> equipment
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemDTO(
            ItemDTOReference item,
            SlotDTO slot,
            String name, // This is the FULL name (e.g. "Bracers of the Whale")
            @JsonProperty("quality") QualityDTO quality,
            @JsonProperty("level") LevelDTO itemLevel, // The actual scaled level
            List<StatDTO> stats, // The actual scaled stats
            ArmorDTO armor,
            WeaponDTO weapon,
            List<EnchantmentDTO> enchantments
    ) {
        public record ItemDTOReference(long id) {
        }

        public record SlotDTO(String type) {
        }

        public record QualityDTO(String type) {
        }

        public record LevelDTO(int value) {
        }

        public record StatDTO(
                @JsonProperty("type") StatTypeWrapper type,
                @JsonProperty("value") int value
        ) {
            public record StatTypeWrapper(String type) {
            }
        }

        public record ArmorDTO(int value) {
        }

        public record WeaponDTO(
                @JsonProperty("damage") DamageDTO damage,
                @JsonProperty("attack_speed") AttackSpeedDTO attackSpeed,
                @JsonProperty("dps") DpsDTO dps
        ) {
            public record DamageDTO(
                    @JsonProperty("min_value") int minValue,
                    @JsonProperty("max_value") int maxValue
            ) {
            }

            public record AttackSpeedDTO(double value) {
            }

            public record DpsDTO(double value) {
            }
        }

        public record EnchantmentDTO(
                @JsonProperty("enchantment_id") long enchantmentId,
                @JsonProperty("display_string") String displayString
        ) {
        }

    }
}
