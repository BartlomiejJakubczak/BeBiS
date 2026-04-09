package com.bebis.BeBiS.integration.blizzard;

import com.bebis.BeBiS.base.BaseWiremockTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest
public class BlizzardUserOAuthIntegrationTest extends BaseWiremockTest {

    @Value("${blizzard.api.namespace.user}")
    private String userNamespace;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository; // "bucket" of configurations (what is Blizzard's URL? What's ClientID?)

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository; // "bucket" for Tokens

    /*
     * this rest client has to have the token already existing in the
     * AuthorizedClientRepository (session)
     */
    @Autowired
    private BlizzardUserClient blizzardUserClient;

    @BeforeEach
    @WithMockUser(username = "test")
    void setUp() {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("blizzard-user");
        OAuth2AccessToken fakeToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "fake-user-token", Instant.now(), Instant.now().plusSeconds(3600));
        OAuth2AuthorizedClient fakeClient = new OAuth2AuthorizedClient(registration, "test", fakeToken);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // bind the request to the current thread context (session)
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        authorizedClientRepository.saveAuthorizedClient(
                fakeClient,
                SecurityContextHolder.getContext().getAuthentication(),
                request,
                response);
    }

    @AfterEach
    void tearDown() {
        // Clear the request context to avoid interference between tests
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldAttachOAuth2TokenToAuthorizationCodeFlowRequest() {
        // given
        stubFor(get(urlPathEqualTo("/profile/user/wow"))
                .withHeader(namespaceHeader, equalTo(userNamespace))
                .withQueryParam("locale", equalTo(locale)) // wiremock ignores everything that is after "?" by default
                .willReturn(okJson("""
                        {"wow_accounts": []}
                        """)));

        // when
        blizzardUserClient.getProfileSummary();

        // then
        verify(getRequestedFor(urlPathEqualTo("/profile/user/wow"))
                .withHeader("Authorization", equalTo("Bearer fake-user-token")));
    }

}
