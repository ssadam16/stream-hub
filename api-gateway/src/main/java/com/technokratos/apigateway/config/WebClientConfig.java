package com.technokratos.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.user-service.uri}")
    private String userServiceBaseUrl;

    @Value("${services.keycloak-service.uri}")
    private String keycloakBaseUrl;

    @Bean
    public WebClient keyCloakWebClient() {
        return WebClient.builder()
                .baseUrl(keycloakBaseUrl)
                .build();
    }

    @Bean
    public WebClient userServiceWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceBaseUrl)
                .build();
    }

}
