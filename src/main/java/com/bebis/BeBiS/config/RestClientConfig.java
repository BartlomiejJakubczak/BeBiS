package com.bebis.BeBiS.config;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RestClientConfig.class);

    @Value("${blizzard.api.base-url}")
    private String baseUrl;

    @Bean
    RestClient restClient(RestClient.Builder builder, OAuth2AuthorizedClientManager authorizedClientManager) {
        ClientHttpRequestInterceptor loggingInterceptor = (request, body, execution) -> {
            log.debug("---------- REQUEST INTERCEPTED ----------");
            log.debug("Request Method : {}", request.getMethod());
            log.debug("Request URI    : {}", request.getURI());
            request.getHeaders().forEach((headerName, headerValues) -> 
                log.debug("Header '{}' : {}", headerName, headerValues));
            ClientHttpResponse response = execution.execute(request, body);
            log.debug("Response Status: {}", response.getStatusCode());
            return response;
        };

        // OAuth2 interceptor provided by Spring Security
        OAuth2ClientHttpRequestInterceptor oauthInterceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);

        return builder
                .baseUrl(baseUrl)
                .requestInterceptor(oauthInterceptor)
                .requestInterceptor(loggingInterceptor)
                .defaultRequest(request -> request.attributes(clientRegistrationId("blizzard")))
                .defaultHeader("Battlenet-Namespace", "static-classic1x-eu")
                .build();
    }
}