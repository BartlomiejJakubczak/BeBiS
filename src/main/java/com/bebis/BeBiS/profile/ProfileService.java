package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ProfileService {

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
        List<CharacterSyncData> fromSummary = profileMapper.mapToSyncData(blizzardClient.getProfileSummary(), blizzardAccountId);
        return synchronizer.synchronize(fromSummary, blizzardAccountId);
    }

}
