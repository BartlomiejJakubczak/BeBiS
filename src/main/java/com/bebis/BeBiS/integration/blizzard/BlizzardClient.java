package com.bebis.BeBiS.integration.blizzard;

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
            .uri("https://eu.api.blizzard.com/data/wow/item/{id}", id)
            .retrieve()
            .body(String.class);
    }
    
}
