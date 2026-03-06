package com.bebis.BeBiS.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient restClient(RestClient.Builder builder, OAuth2AuthorizedClientManager authorizedClientManager) {
        // Logging interceptor
        ClientHttpRequestInterceptor loggingInterceptor = (request, body, execution) -> {
            System.out.println("---------- REQUEST INTERCEPTED ----------");
            System.out.println("Request method: " + request.getMethod());
            System.out.println("Request URI: " + request.getURI());
            request.getHeaders().forEach((k,v) -> System.out.println(k + "=" + v));
            ClientHttpResponse response = execution.execute(request, body);
            System.out.println(response.getStatusText());
            System.out.println(response.getHeaders());
            return response;
        };

        // OAuth2 interceptor provided by Spring Security
        OAuth2ClientHttpRequestInterceptor oauthInterceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);

        return builder
                .baseUrl("https://eu.api.blizzard.com")
                .requestInterceptor(oauthInterceptor)
                .requestInterceptor(loggingInterceptor)
                .defaultHeader("Battlenet-Namespace", "static-classic1x-eu")
                .build();
    }
}