package com.technokratos.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${user-service.uri}")
    private String userServiceUri;

    @Bean
    public WebClient authWebClient() {
        log.info("USER_SERVICE_URI: {}", userServiceUri);
        return WebClient.builder()
                .baseUrl(userServiceUri)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter((request, next) -> {
                    log.debug("Calling AUTH-SERVICE: {} {}", request.method(), request.url());
                    return next.exchange(request);
                })
                .build();
    }


}
