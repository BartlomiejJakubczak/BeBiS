package com.bebis.BeBiS.integration.blizzard;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bebis.BeBiS.items.Item;

@Component
public class BlizzardClient {

    public static final String LOCALE_QUERY_PARAM = "?locale=en_GB";

    private final RestClient restClient;

    public BlizzardClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Item getItem(int id) {
        return this.restClient.get()
            .uri("/data/wow/item/{id}" + LOCALE_QUERY_PARAM, id)
            .retrieve()
            .body(Item.class);
    }
    
}
