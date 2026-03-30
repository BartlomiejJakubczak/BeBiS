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
            SlotDTO slot
    ) {
        public record ItemDTOReference(long id) {
        }

        public record SlotDTO(String type) {
        }
    }
}
