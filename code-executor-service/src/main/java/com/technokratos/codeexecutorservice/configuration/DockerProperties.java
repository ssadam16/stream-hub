package com.technokratos.codeexecutorservice.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "docker.http-client")
public class DockerProperties {

    private Integer maxConnections;

    private Integer connectionTimeout;

    private Integer responseTimeout;

    @Value("${docker.images.java}")
    private String javaImage;
}