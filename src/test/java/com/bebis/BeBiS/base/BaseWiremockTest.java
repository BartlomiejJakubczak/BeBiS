package com.bebis.BeBiS.base;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

/*
    // Use this for OAuth2 flows because a real HTTP port is needed for the token exchange.
 */

@ActiveProfiles("wiremock")
@WireMockTest(httpPort = 8089) // Start WireMock on the fixed port, external reasource not interacting with Spring
public abstract class BaseWiremockTest {

    @Value("${blizzard.api.namespace.header}")
    protected String namespaceHeader;

    @Value("${blizzard.api.locale}")
    protected String locale;

}
