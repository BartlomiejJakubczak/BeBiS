package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowRealm;
import com.bebis.BeBiS.profile.domain.exception.InvalidCharacterException;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.bebis.BeBiS.tools.MapperTools.validateRequired;

@Component
class ProfileMapper {

    public List<WowCharacterDTO> mapToDTOs(ProfileSummaryResponse response) {
        if (response == null) return List.of();
        return Optional.ofNullable(response.wowAccounts())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(WowAccountDTO::characters)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
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
    public CharacterSyncData fromDTO(WowCharacterDTO dto, long blizzardAccountId) throws InvalidCharacterException {
        return new CharacterSyncData(
                validateRequired(dto.wowCharacterId(), "id", InvalidCharacterException::new),
                mapRealmSlug(dto),
                blizzardAccountId,
                dto.name() != null ? dto.name() : "",
                validateRequired(dto.level(), "level", InvalidCharacterException::new),
                mapRace(dto),
                mapWowClass(dto),
                mapRealmName(dto)
        );
    }

    private String mapRealmSlug(WowCharacterDTO dto) {
        if (dto.realm() != null && dto.realm().slug() != null) return dto.realm().slug();
        throw new InvalidCharacterException("realmSlug cannot be null for entity identification purposes");
    }

    private WowCharacter.Race mapRace(WowCharacterDTO dto) {
        if (dto.race() != null && dto.race().name() != null) {
            try {
                return WowCharacter.Race.fromBlizzardName(dto.race().name());
            } catch (IllegalArgumentException e) {
                throw new InvalidCharacterException("invalid race, message: \n" + e.getMessage());
            }
        }
        throw new InvalidCharacterException("race cannot be null for upgrade analysis purposes");
    }

    private WowCharacter.WowClass mapWowClass(WowCharacterDTO dto) {
        if (dto.wowClass() != null && dto.wowClass().name() != null) {
            try {
                return WowCharacter.WowClass.valueOf(dto.wowClass().name());
            } catch (IllegalArgumentException e) {
                throw new InvalidCharacterException("invalid wowClass, message: \n" + e.getMessage());
            }
        }
        throw new InvalidCharacterException("wowClass cannot be null for upgrade analysis purposes");
    }

    private String mapRealmName(WowCharacterDTO dto) {
        if (dto.realm() != null && dto.realm().name() != null) return dto.realm().name();
        return "";
    }

}
