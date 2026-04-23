package com.technokratos.solutionservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

    @Value("${task-service-uri}")
    private String taskServiceUri;

    @Bean
    public RestClient taskServiceRestClient() {
        return RestClient.builder()
                .baseUrl(taskServiceUri)
                .build();
    }

}
