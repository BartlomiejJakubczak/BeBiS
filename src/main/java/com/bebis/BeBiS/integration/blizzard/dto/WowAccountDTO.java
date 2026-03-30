package com.bebis.BeBiS.integration.blizzard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WowAccountDTO(
        @JsonProperty("id") long wowAccountId,
        List<WowCharacterDTO> characters
) {
}
