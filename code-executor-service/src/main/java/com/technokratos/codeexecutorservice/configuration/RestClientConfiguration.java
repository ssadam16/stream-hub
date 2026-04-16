package com.technokratos.codeexecutorservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Configuration
public class RestClientConfiguration {

    @Value("${services.task-service.uri}")
    private String taskServiceUri;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(URI.create(taskServiceUri))
                .build();
    }

}
