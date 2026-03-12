package com.bebis.BeBiS.integration.blizzard;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BlizzardUserClient {

    private final RestClient restClient;

    public BlizzardUserClient(@Qualifier("blizzardUserRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public String getProfileSummary() {
        return this.restClient.get()
            .uri("/profile/user/wow")
            .retrieve()
            .body(String.class);
    }

}
