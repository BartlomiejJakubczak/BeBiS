package com.bebis.BeBiS.profile;

import org.springframework.stereotype.Service;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;

@Service
public class ProfileService {

    private final BlizzardUserClient blizzardClient;

    public ProfileService(BlizzardUserClient blizzardClient) {
        this.blizzardClient = blizzardClient;
    }

    public String getProfileSummary() {
        return blizzardClient.getProfileSummary();
    }

}
