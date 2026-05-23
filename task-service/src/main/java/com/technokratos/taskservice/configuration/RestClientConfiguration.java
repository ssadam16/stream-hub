package com.technokratos.taskservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

    @Value("${services.solution-service.uri}")
    private String solutionServiceUri;

    @Bean
    public RestClient solutionRestClient() {
        return RestClient.builder()
                .baseUrl(solutionServiceUri)
                .build();
    }
}
