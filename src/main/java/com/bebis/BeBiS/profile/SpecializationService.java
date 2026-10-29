package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.domain.exception.InvalidTalentTreeException;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;


@Service
class SpecializationService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SpecializationService.class);

    private final BlizzardSpecializationFetcher specializationFetcher;
    private final SpecializationMapper specMapper;

    SpecializationService(BlizzardSpecializationFetcher specializationFetcher, SpecializationMapper specMapper) {
        this.specializationFetcher = specializationFetcher;
        this.specMapper = specMapper;
    }

    /**
     * Fetches the character's talents from Blizzard. A failed fetch falls back to an empty response, so it reads the
     * same as a character with no talents: both get class-generic stat weights.
     */
    public WowTalents getTalentsForCharacter(String realmSlug, String characterName) {
        try {
            return specMapper.fromDTO(specializationFetcher.fetchSpecialization(realmSlug, characterName));
        } catch (InvalidTalentTreeException e) {
            log.error("Invalid specialization for: {}, reason: {}", characterName, e.getMessage());
            return WowTalents.none();
        }
    }
}
