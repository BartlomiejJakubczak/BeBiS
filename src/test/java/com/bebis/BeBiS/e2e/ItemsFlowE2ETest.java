package com.bebis.BeBiS.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ItemsFlowE2ETest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldFetchThunderfuryFromRealBlizzard() {

        String response =
            restTemplate.getForObject("/api/items/19019?locale=en_GB", String.class);

        assertTrue(response.contains("Thunderfury"));
    }
    
}
