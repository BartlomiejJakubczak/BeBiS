package com.bebis.BeBiS.integration.blizzard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestClient;

@SpringBootTest
class BlizzardClientTest {

    @Autowired
    RestClient.Builder builder;

    private MockRestServiceServer server;

    private BlizzardClient blizzardClient;

    @BeforeEach
    void setup() {
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        blizzardClient = new BlizzardClient(restClient);
    }

    @Test
    void shouldGetItemWithCorrectPath() {
        // when
        server.expect(requestTo("/data/wow/item/2137?locale=en_GB"))
                .andRespond(withSuccess("item-data", MediaType.APPLICATION_JSON));

        // then
        String result = blizzardClient.getItem(2137);
        assertEquals("item-data", result);
    }

}