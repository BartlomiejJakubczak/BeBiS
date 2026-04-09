package com.bebis.BeBiS.integration.blizzard;

import com.bebis.BeBiS.base.BaseWiremockTest;
import com.bebis.BeBiS.item.ItemTestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class BlizzardServiceOAuthIntegrationTest extends BaseWiremockTest {

    @Value("${blizzard.api.namespace.service}")
    private String serviceNamespace;

    @Autowired
    private BlizzardServiceClient blizzardClient;

    @Test
    void shouldFetchItem() {
        // given
        stubFor(post("/token").willReturn(okJson("""
                {"access_token": "fake-token", "token_type": "bearer", "expires_in": 3600}""")));

        stubFor(get(urlPathEqualTo("/data/wow/item/" + ItemTestData.THUNDERFURY_ID))
                .withHeader(namespaceHeader, equalTo(serviceNamespace))
                .withQueryParam("locale", equalTo(locale)) // wiremock ignores everything that is after "?" by default
                .willReturn(okJson("""
                        {"id": 19019, "name": "Thunderfury"}""")));

        // when
        var item = blizzardClient.getBaseItem(ItemTestData.THUNDERFURY_ID);

        // then
        assertTrue(item.name().contains("Thunderfury"));
        // Verify the OAuth interceptor actually sent the token
        verify(getRequestedFor(urlPathEqualTo("/data/wow/item/" + ItemTestData.THUNDERFURY_ID))
                .withHeader("Authorization", equalTo("Bearer fake-token")));
    }
}