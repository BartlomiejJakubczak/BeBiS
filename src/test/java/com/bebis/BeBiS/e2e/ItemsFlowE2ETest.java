package com.bebis.BeBiS.e2e;

import com.bebis.BeBiS.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class ItemsFlowE2ETest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate; // "browser in a box"

    @Test
    void shouldFetchThunderfuryFromRealBlizzard() {

        String response =
                restTemplate.getForObject("/api/items/19019", String.class);

        assertTrue(response.contains("Thunderfury"));
    }

}
