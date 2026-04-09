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
            String name,
            List<EnchantmentDTO> enchantments
            // could mean a suffix item ("of the X"), but also player ench like Crusader
    ) {
        public record ItemDTOReference(long id) {
        }

        public record SlotDTO(String type) {
        }

        public record EnchantmentDTO(
                @JsonProperty("enchantment_id") long enchantmentId,
                @JsonProperty("display_string") String displayString
        ) {
        }

        public long getSuffixId() {
            if (this.enchantments() == null) {
                return 0L;
            }
            return this.enchantments().stream()
                    .filter((ench) -> this.name().contains(ench.displayString()))
                    .map(EnchantmentDTO::enchantmentId)
                    .findFirst()
                    .orElse(0L);
        }

        public List<String> getPlayerEnchantStrings() {
            if (enchantments == null) return List.of();
            long suffixId = getSuffixId();
            return enchantments.stream()
                    .filter(ench -> ench.enchantmentId() != suffixId)
                    .map(EnchantmentDTO::displayString)
                    .toList();
        }
    }
}
