package com.bebis.BeBiS.integration.blizzard;

import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BlizzardUserClient {

    public static final String LOCALE_QUERY_PARAM = "?locale=en_GB";

    private final RestClient restClient;

    public BlizzardUserClient(@Qualifier("blizzardUserRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ProfileSummaryResponse getProfileSummary() {
        return restClient.get()
                .uri("/profile/user/wow" + LOCALE_QUERY_PARAM)
                .retrieve()
                .body(ProfileSummaryResponse.class);
    }

    public EquipmentResponse getCharacterEquipment(String realmSlug, String characterName) {
        return restClient.get()
                .uri("/profile/wow/character/" + realmSlug + "/" + characterName + "/equipment" + LOCALE_QUERY_PARAM)
                .retrieve()
                .body(EquipmentResponse.class);
    }

}
