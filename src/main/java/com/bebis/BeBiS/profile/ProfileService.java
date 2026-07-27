package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.exception.InvalidCharacterException;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
class ProfileService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ProfileService.class);

    private final BlizzardUserClient blizzardClient;
    private final ProfileSynchronizer synchronizer;
    private final ProfileMapper profileMapper;

    ProfileService(
            BlizzardUserClient blizzardClient,
            ProfileSynchronizer synchronizer,
            ProfileMapper profileMapper) {
        this.blizzardClient = blizzardClient;
        this.synchronizer = synchronizer;
        this.profileMapper = profileMapper;
    }

    public List<WowCharacter> getProfileSummary(long blizzardAccountId) {
        return profileMapper.mapToDomain(syncBlizzardAccountCharacters(blizzardAccountId));
    }

    private List<WowCharacterEntity> syncBlizzardAccountCharacters(long blizzardAccountId) {
        List<WowCharacterDTO> fromSummary = profileMapper.mapToDTOs(blizzardClient.getProfileSummary());
        List<CharacterSyncData> syncData = new ArrayList<>();
        for (WowCharacterDTO dto : fromSummary) {
            try {
                syncData.add(profileMapper.fromDTO(dto, blizzardAccountId));
            } catch (InvalidCharacterException e) {
                log.error("Invalid character data for {}, reason: {}", blizzardAccountId, e.getMessage());
            }
        }
        return synchronizer.synchronize(syncData, blizzardAccountId);
    }

}
