package com.technokratos.codeexecutorservice.configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class DockerClientConfiguration {

    private final DockerProperties dockerProperties;

    @Bean
    public DockerClient dockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerConfig(getDockerHost())
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(dockerProperties.getMaxConnections())
                .connectionTimeout(Duration.ofSeconds(dockerProperties.getConnectionTimeout()))
                .responseTimeout(Duration.ofSeconds(dockerProperties.getResponseTimeout()))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    private String getDockerHost() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "npipe:////./pipe/docker_engine";
        } else if (os.contains("mac")) {
            return "unix:///var/run/docker.sock";
        } else {
            return "unix:///var/run/docker.sock";
        }
    }
}
