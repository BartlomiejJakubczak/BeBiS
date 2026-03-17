package com.bebis.BeBiS.integration.blizzard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A Domain-level representation of a WoW Item.
 * Note: @JsonIgnoreProperties(ignoreUnknown = true) is actually the default for Spring's RestClient,
 * but being explicit here helps others see we are intentionally cherry-picking from Blizzard's JSON.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public record Item(
        long id,
        String name
) {
}
