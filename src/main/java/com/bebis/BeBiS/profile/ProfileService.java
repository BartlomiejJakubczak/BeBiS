package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {

    private final BlizzardUserClient blizzardClient;
    private final ProfileMapper profileMapper;

    public ProfileService(BlizzardUserClient blizzardClient, ProfileMapper profileMapper) {
        this.blizzardClient = blizzardClient;
        this.profileMapper = profileMapper;
    }

    public List<WowCharacter> getProfileSummary() {
        return profileMapper.mapToDomain(blizzardClient.getProfileSummary());
    }

}
