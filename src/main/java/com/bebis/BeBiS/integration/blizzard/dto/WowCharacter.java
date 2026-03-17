package com.bebis.BeBiS.integration.blizzard.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WowCharacter(
        long id,
        String name,
        int level,
        HrefLink character,
        Realm realm,
        @JsonProperty("playable_race") Race race,
        @JsonProperty("playable_class") WoWClass wowClass
) {
}
