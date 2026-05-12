package com.technokratos.solutionservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class RestClientConfiguration {

    @Value("${task-service-uri}")
    private String taskServiceUri;

    @Bean
    public RestClient taskServiceRestClient() {
        log.info("{}, {}", taskServiceUri, taskServiceUri + "/ID");
        return RestClient.builder()
                .baseUrl(taskServiceUri)
                .build();
    }

}
