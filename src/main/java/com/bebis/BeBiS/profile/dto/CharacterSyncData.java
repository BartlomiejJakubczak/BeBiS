package com.bebis.BeBiS.profile.dto;

import com.bebis.BeBiS.profile.domain.WowCharacter;

/*
    A bridge data class between my dtos and persistence level.
 */

public record CharacterSyncData(
        long characterId,
        String realmSlug,
        long blizzardAccountId,
        String name,
        int level,
        WowCharacter.Race race,
        WowCharacter.WowClass wowClass,
        String realmName
) {
}
