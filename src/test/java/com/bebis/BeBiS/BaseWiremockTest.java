package com.bebis.BeBiS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@ActiveProfiles("wiremock")
@WireMockTest(httpPort = 8089) // Start WireMock on the fixed port
public abstract class BaseWiremockTest extends BaseIntegrationTest {

    @Value("${blizzard.api.namespace.header}")
    protected String namespaceHeader;

    @Value("${blizzard.api.locale}")
    protected String locale;

}
