package com.bebis.BeBiS.integration.blizzard;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.bebis.BeBiS.BaseWiremockTest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

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

        stubFor(get(urlPathEqualTo("/data/wow/item/19019"))
                        .withHeader(namespaceHeader, equalTo(serviceNamespace))
                        .withQueryParam("locale", equalTo(locale)) // wiremock ignores everything that is after "?" by default
                        .willReturn(okJson("""
                                {"id": 19019, "name": "Thunderfury"}""")));

        // when
        var item = blizzardClient.getItem(19019);

        // then
         assertTrue(item.name().contains("Thunderfury"));
        // Verify the OAuth interceptor actually sent the token
        verify(getRequestedFor(urlPathEqualTo("/data/wow/item/19019"))
                .withHeader("Authorization", equalTo("Bearer fake-token")));
        }
}