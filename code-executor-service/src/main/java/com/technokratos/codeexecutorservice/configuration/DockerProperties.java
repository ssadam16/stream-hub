package com.technokratos.codeexecutorservice.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "docker")
public class DockerProperties {

    private HttpClient httpClient = new HttpClient();

    private Images images = new Images();

    @Getter
    @Setter
    public static class HttpClient {
        private Integer maxConnections;
        private Integer connectionTimeout;
        private Integer responseTimeout;
    }

    @Getter
    @Setter
    public static class Images {
        private String java;
        private String python;
        private String javascript;
        private String cpp;
        private String go;
    }
}