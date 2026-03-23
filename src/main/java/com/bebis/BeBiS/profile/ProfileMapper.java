package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProfileMapper {

    public List<WowCharacter> mapToDomain(ProfileSummaryResponse response) {
        return Optional.ofNullable(response.wowAccounts())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(WowAccountDTO::characters)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(this::fromDTO)
                .toList();
    }

    private WowCharacter fromDTO(WowCharacterDTO dto) {
        return new WowCharacter(
                new WowCharacter.Id(dto.id(), dto.realm().slug()),
                dto.name(),
                dto.level(),
                WowCharacter.Race.valueOf(dto.race().name()),
                WowCharacter.WowClass.valueOf(dto.wowClass().name()),
                new WowRealm(dto.realm().name(), dto.realm().slug())
        );
    }

}
