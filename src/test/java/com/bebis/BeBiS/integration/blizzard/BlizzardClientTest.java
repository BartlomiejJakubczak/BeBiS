package com.bebis.BeBiS.integration.blizzard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bebis.BeBiS.config.RestClientConfig;
import com.bebis.BeBiS.item.Item;
import com.bebis.BeBiS.items.ItemTestData;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestClientTest(BlizzardClient.class)
@Import(RestClientConfig.class)
class BlizzardClientTest {

    @Autowired
    private BlizzardClient blizzardClient;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Test
    void shouldGetItemWithCorrectPath() throws Exception {
        // given
        Item thunderfury = ItemTestData.thunderfury();
        String jsonResponse = objectMapper.writeValueAsString(thunderfury);

        // when
        server.expect(requestTo("https://eu.api.blizzard.com/data/wow/item/" + thunderfury.id() + BlizzardClient.LOCALE_QUERY_PARAM))
              .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Item result = blizzardClient.getItem(thunderfury.id());

        // then
        assertEquals(thunderfury, result);
    }
}