package com.bebis.BeBiS.integration.blizzard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Production DTO mapping the WoW Classic Profile Specializations endpoint response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpecializationResponse(
        @JsonProperty("_links") Links links,
        @JsonProperty("specializations") List<CharacterSpecialization> specializations,
        @JsonProperty("character") CharacterReference character
) {
    public record Links(
            @JsonProperty("self") HrefLinkDTO self
    ) {
    }

    public record CharacterSpecialization(
            @JsonProperty("specialization") SpecializationReference specialization,
            @JsonProperty("talents") List<TalentDetails> talents,
            @JsonProperty("points_spent") Integer pointsSpent
    ) {
    }

    public record SpecializationReference(
            @JsonProperty("key") HrefLinkDTO key,
            @JsonProperty("name") String name,
            @JsonProperty("id") Long id
    ) {
    }

    public record TalentDetails(
            @JsonProperty("talent") TalentReference talent,
            @JsonProperty("spell_tooltip") SpellTooltip spellTooltip
    ) {
    }

    public record TalentReference(
            @JsonProperty("key") HrefLinkDTO key,
            @JsonProperty("name") String name,
            @JsonProperty("id") Long id
    ) {
    }

    public record SpellTooltip(
            @JsonProperty("spell") SpellReference spell,
            @JsonProperty("description") String description
    ) {
    }

    public record SpellReference(
            @JsonProperty("key") HrefLinkDTO key,
            @JsonProperty("name") String name,
            @JsonProperty("id") Long id
    ) {
    }

    public record CharacterReference(
            @JsonProperty("key") HrefLinkDTO key,
            @JsonProperty("name") String name,
            @JsonProperty("id") Long id,
            @JsonProperty("realm") RealmReference realm
    ) {
    }

    public record RealmReference(
            @JsonProperty("key") HrefLinkDTO key,
            @JsonProperty("name") String name,
            @JsonProperty("id") Long id,
            @JsonProperty("slug") String slug
    ) {
    }
}