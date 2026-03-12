package com.bebis.BeBiS.integration.blizzard;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestClient;

import com.bebis.BeBiS.config.RestClientConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestClientTest(BlizzardUserClient.class) // sets up OUTGOING HTTP tools, so web server is gone
@Import(RestClientConfig.class) // pulls 2 RestClient beans, hence elaborate setup
public class BlizzardUserClientTest {

    @Value("${blizzard.api.base-url}")
    private String baseUrl;
    
    @Autowired
    @Qualifier("blizzardUserRestClient")
    private RestClient userRestClient;
    
    @MockitoBean(name = "userManager") // mock the whole manager to avoid the need for a real HttpServletRequest
    private DefaultOAuth2AuthorizedClientManager userManager;
    
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
    
    @MockitoBean private OAuth2AuthorizedClientRepository authorizedClientRepository;
    
    @MockitoBean private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockRestServiceServer server;
    private BlizzardUserClient blizzardUserClient;

    @BeforeEach
    @WithMockUser(username = "test") // Simulate an authenticated user for the test
    void setUp() {
        // DefaultOAuth2AuthorizedClientManager is hardcoded to look at the user's browser session to find the token
        // Because there is no web server running in this sliced test, the HttpServletRequest is null

        OAuth2AccessToken fakeToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "fake-user-token", Instant.now(), Instant.now().plusSeconds(3600));

        ClientRegistration dummyRegistration = ClientRegistration.withRegistrationId("blizzard-user")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("dummy")
                .tokenUri("https://dummy")
                .redirectUri("https://dummy-redirect")
                .authorizationUri("https://dummy-auth")
                .build();

        OAuth2AuthorizedClient fakeAuthorizedClient = new OAuth2AuthorizedClient(
                dummyRegistration, "test-user", fakeToken);

        // mock the whole thing
        when(userManager.authorize(any())).thenReturn(fakeAuthorizedClient);
        
        RestClient.Builder builder = userRestClient.mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        blizzardUserClient = new BlizzardUserClient(builder.build());
    }

    @Test
    void shouldGetProfileSummaryWithCorrectPath() throws Exception {
        // given
        String expectedSummary = "Profile Summary";
        server.expect(requestTo("https://eu.api.blizzard.com/profile/user/wow" + BlizzardUserClient.LOCALE_QUERY_PARAM))
              // Optional: prove the interceptor attached the token from our mocked manager!
              .andExpect(header("Authorization", "Bearer fake-user-token")) 
              .andRespond(withSuccess(expectedSummary, MediaType.APPLICATION_JSON));

        // when
        String response = blizzardUserClient.getProfileSummary();

        // then
        assertEquals(expectedSummary, response);
        server.verify();
    }
    

}
