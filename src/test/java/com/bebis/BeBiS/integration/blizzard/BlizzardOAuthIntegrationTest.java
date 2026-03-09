package com.bebis.BeBiS.integration.blizzard;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@SpringBootTest
@ActiveProfiles("test")              // Use application-test.yml
@WireMockTest(httpPort = 8089)       // Start WireMock on the fixed port
class BlizzardOAuthIntegrationTest {

    @Autowired
    private BlizzardClient blizzardClient;

    @Test
    void shouldFetchItem() {
        // given
        stubFor(post("/token").willReturn(okJson("""
                {"access_token": "fake-token", "token_type": "bearer", "expires_in": 3600}
                """)));

        stubFor(get(urlPathEqualTo("/data/wow/item/19019"))
                .willReturn(okJson("""
                {"id": 19019, "name": "Thunderfury"}
                """)));

        // when
        var item = blizzardClient.getItem(19019);

        // then
        assertTrue(item.name().contains("Thunderfury"));
        // Verify the OAuth interceptor actually sent the token
        verify(getRequestedFor(urlPathEqualTo("/data/wow/item/19019"))
                .withHeader("Authorization", equalTo("Bearer fake-token")));
    }
}