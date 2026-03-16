package com.bebis.BeBiS.config;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@Configuration
public class RestClientConfig {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RestClientConfig.class);

    @Value("${blizzard.api.base-url}")
    private String baseUrl;

    @Value("${blizzard.api.namespace.header}")
    private String namespaceHeader;

    @Bean
    ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("---------- REQUEST INTERCEPTED ----------");
            log.debug("Request Method : {}", request.getMethod());
            log.debug("Request URI    : {}", request.getURI());
            request.getHeaders()
                    .forEach((headerName, headerValues) -> log.debug("Header '{}' : {}", headerName, headerValues));
            ClientHttpResponse response = execution.execute(request, body);
            log.debug("Response Status: {}", response.getStatusCode());
            return response;
        };
    }

    @Bean
        // overrides default web browser based OAuth2AuthorizedClientManager with a
        // background-service manager
    OAuth2AuthorizedClientManager serviceManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
    }

    @Bean
    RestClient blizzardServiceRestClient(@Value("${blizzard.api.namespace.service}") String serviceNamespace,
                                         RestClient.Builder builder,
                                         OAuth2AuthorizedClientManager serviceManager,
                                         ClientHttpRequestInterceptor loggingInterceptor) {

        OAuth2ClientHttpRequestInterceptor oauthInterceptor = new OAuth2ClientHttpRequestInterceptor(
                serviceManager);

        return builder
                .baseUrl(baseUrl)
                .requestInterceptor(oauthInterceptor)
                .requestInterceptor(loggingInterceptor)
                .defaultRequest(request -> request.attributes(clientRegistrationId("blizzard-service")))
                .defaultHeader(namespaceHeader, serviceNamespace)
                .build();
    }

    @Bean
    DefaultOAuth2AuthorizedClientManager userManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        return new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository);
    }

    @Bean
    RestClient blizzardUserRestClient(@Value("${blizzard.api.namespace.user}") String clientNamespace,
                                      RestClient.Builder builder,
                                      DefaultOAuth2AuthorizedClientManager userManager,
                                      ClientHttpRequestInterceptor loggingInterceptor) {

        OAuth2ClientHttpRequestInterceptor oauthInterceptor = new OAuth2ClientHttpRequestInterceptor(
                userManager);

        return builder
                .baseUrl(baseUrl)
                .requestInterceptor(oauthInterceptor)
                .requestInterceptor(loggingInterceptor)
                .defaultRequest(request -> request.attributes(clientRegistrationId("blizzard-user")))
                .defaultHeader(namespaceHeader, clientNamespace)
                .build();

    }

}