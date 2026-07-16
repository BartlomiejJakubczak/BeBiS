package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class SpecializationService {

    private final BlizzardUserClient blizzardClient;
    private final SpecializationMapper specMapper;

    public SpecializationService(BlizzardUserClient blizzardClient, SpecializationMapper specMapper) {
        this.blizzardClient = blizzardClient;
        this.specMapper = specMapper;
    }

    public Optional<WowTalents> getTalentsForCharacter(String realmSlug, String characterName) {
        return specMapper.fromDTO(blizzardClient.getCharacterSpecialization(realmSlug, characterName));
    }
}
