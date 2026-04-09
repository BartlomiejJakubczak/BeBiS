package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProfileMapper {

    public List<CharacterSyncData> mapToSyncData(ProfileSummaryResponse response, long blizzardAccountId) {
        return Optional.ofNullable(response.wowAccounts())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(WowAccountDTO::characters)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(wowCharacterDTO -> fromDTO(wowCharacterDTO, blizzardAccountId))
                .toList();
    }

    public List<WowCharacter> mapToDomain(List<WowCharacterEntity> entities) {
        return Optional.ofNullable(entities)
                .orElse(List.of())
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    public WowCharacter mapToDomain(WowCharacterEntity entity) {
        return new WowCharacter(
                new WowCharacter.Id(entity.getPk().getId(), entity.getPk().getRealmSlug()),
                entity.getName(),
                entity.getLevel(),
                entity.getRace(),
                entity.getWowClass(),
                new WowRealm(entity.getRealmName(), entity.getRealmName())
        );
    }

    // will have to test it eventually as I made it public, or develop some testing classes for generating Entities
    public CharacterSyncData fromDTO(WowCharacterDTO dto, long blizzardAccountId) {
        return new CharacterSyncData(
                dto.wowCharacterId(),
                dto.realm().slug(),
                blizzardAccountId,
                dto.name(),
                dto.level(),
                WowCharacter.Race.fromBlizzardName(dto.race().name()),
                WowCharacter.WowClass.valueOf(dto.wowClass().name().toUpperCase()),
                dto.realm().name()
        );
    }
}
