package com.technokratos.codeexecutorservice.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@ConfigurationProperties(prefix = "docker.http-client")
public class DockerProperties {

    @Value("${docker.http-client.max-connections}")
    private Integer maxConnections;

    @Value("${docker.http-client.connection-timeout}")
    private Integer connectionTimeout;

    @Value("${docker.http-client.response-timeout}")
    private Integer responseTimeout;
}