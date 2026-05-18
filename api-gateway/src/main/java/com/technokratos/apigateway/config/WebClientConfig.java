package com.technokratos.apigateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

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
                .clientConnector(new ReactorClientHttpConnector(defaultHttpClient()))
                .build();
    }


    private HttpClient defaultHttpClient() {
        return HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
    }
}
