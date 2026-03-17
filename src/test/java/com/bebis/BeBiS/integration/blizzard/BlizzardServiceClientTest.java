package com.bebis.BeBiS.integration.blizzard;

import com.bebis.BeBiS.config.RestClientConfig;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.ItemTestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(BlizzardServiceClient.class)
@Import(RestClientConfig.class) // pulls 2 RestClient beans, hence elaborate setup
class BlizzardServiceClientTest {

    private BlizzardServiceClient blizzardClient;

    @Autowired
    @Qualifier("blizzardServiceRestClient") // no web requests needed here as this RestClient is a background service
    // that doesnt look at web browser based security contexts, so we can just mock the manager to return a pre-baked token
    private RestClient serviceRestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Value("${blizzard.api.base-url}")
    private String baseUrl;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        ClientRegistration dummyRegistration = ClientRegistration.withRegistrationId("blizzard-service")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientId("dummy-client")
                .tokenUri("https://dummy/token")
                .build();

        when(clientRegistrationRepository.findByRegistrationId("blizzard-service"))
                .thenReturn(dummyRegistration);

        // Create a fake "Authorized Client" with a pre-baked token
        OAuth2AccessToken fakeToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "fake-service-token",
                Instant.now(),
                Instant.now().plusSeconds(3600));

        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                dummyRegistration,
                "anonymousUser", // Default principal for service-to-service oauth2
                fakeToken);

        // Tell the service to return this client so the Manager doesn't try to fetch a new one
        when(authorizedClientService.loadAuthorizedClient("blizzard-service", "anonymousUser"))
                .thenReturn(authorizedClient);

        RestClient.Builder builder = serviceRestClient.mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        blizzardClient = new BlizzardServiceClient(builder.build());
    }

    @Test
    void shouldGetItemWithCorrectPath() throws Exception {
        // given
        ItemResponse thunderfury = ItemTestData.thunderfury();
        String jsonResponse = objectMapper.writeValueAsString(thunderfury);

        // when
        server.expect(requestTo(baseUrl + "/data/wow/item/" + thunderfury.id() + BlizzardServiceClient.LOCALE_QUERY_PARAM))
                .andExpect(header("Authorization", "Bearer fake-service-token"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ItemResponse result = blizzardClient.getItem(thunderfury.id());

        // then
        assertEquals(thunderfury, result);
        server.verify();
    }
}