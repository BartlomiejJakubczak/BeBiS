package com.bebis.BeBiS.integration.blizzard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Production DTO mapping the WoW Classic Era Profile Specializations endpoint response.
 * Only the fields we read are mapped; the full shape lives in the recordings (src/test/resources/blizzard/specs).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpecializationResponse(
        // up to two groups (dual spec), and the active one isn't always first
        @JsonProperty("specialization_groups") List<SpecializationGroup> specializationGroups
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpecializationGroup(
            @JsonProperty("is_active") boolean isActive,
            // absent when no points are spent; trees with 0 points are left out
            @JsonProperty("specializations") List<Specialization> specializations
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Specialization(
            @JsonProperty("specialization_name") String name, // localized tree name, e.g. "Feral Combat"
            @JsonProperty("spent_points") int spentPoints
    ) {
    }
}
