package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.profile.domain.WowTalents;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
class SpecializationService {

    private final BlizzardSpecializationFetcher specializationFetcher;
    private final SpecializationMapper specMapper;

    SpecializationService(BlizzardSpecializationFetcher specializationFetcher, SpecializationMapper specMapper) {
        this.specializationFetcher = specializationFetcher;
        this.specMapper = specMapper;
    }

    public Optional<WowTalents> getTalentsForCharacter(String realmSlug, String characterName) {
        return specMapper.fromDTO(specializationFetcher.fetchSpecialization(realmSlug, characterName));
    }
}
