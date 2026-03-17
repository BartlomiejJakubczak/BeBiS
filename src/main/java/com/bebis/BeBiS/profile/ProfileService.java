package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummary;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final BlizzardUserClient blizzardClient;

    public ProfileService(BlizzardUserClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    public ProfileSummary getProfileSummary() {
        return blizzardClient.getProfileSummary();
    }

}
