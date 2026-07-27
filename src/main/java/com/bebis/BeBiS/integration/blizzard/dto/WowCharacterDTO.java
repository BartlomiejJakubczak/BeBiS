package com.bebis.BeBiS.integration.blizzard.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WowCharacterDTO(
        @JsonProperty("id") Long wowCharacterId,
        String name,
        Integer level,
        HrefLinkDTO character,
        RealmDTO realm,
        @JsonProperty("playable_race") RaceDTO race,
        @JsonProperty("playable_class") WowClassDTO wowClass
) {
}
