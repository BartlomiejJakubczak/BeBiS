package com.bebis.BeBiS.integration.blizzard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfileSummaryResponse(
        long id,
        @JsonProperty("wow_accounts") List<WowAccountDTO> wowAccounts
) {
}
