package com.bebis.BeBiS.integration.blizzard;

import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BlizzardClient {

    private final RestClient restClient;

    public BlizzardClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String getItem(int id) {
        return this.restClient.get()
            .uri("/data/wow/item/{id}?locale=en_GB", id)
            .attributes(RequestAttributeClientRegistrationIdResolver.clientRegistrationId("blizzard"))
            .retrieve()
            .body(String.class);
    }
    
}
