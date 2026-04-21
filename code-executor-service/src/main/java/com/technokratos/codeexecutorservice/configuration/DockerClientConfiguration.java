package com.technokratos.codeexecutorservice.configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DockerClientConfiguration {

    private final DockerProperties dockerProperties;

    @Bean
    public DockerClient dockerClient() {

        String dockerHost = getDockerHost();

        log.info("Configuring Docker client with host: {}", dockerHost);

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(dockerProperties.getHttpClient().getMaxConnections())
                .connectionTimeout(Duration.ofSeconds(dockerProperties.getHttpClient().getConnectionTimeout()))
                .responseTimeout(Duration.ofSeconds(dockerProperties.getHttpClient().getResponseTimeout()))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    private String getDockerHost() {
        String os = System.getProperty("os.name").toLowerCase();
        log.debug("Detected OS: {}", os);

        String dockerHost;
        if (os.contains("win")) {
            dockerHost = "npipe:////./pipe/docker_engine";
            log.info("Windows detected, using named pipe for Docker");
        } else if (os.contains("mac")) {
            dockerHost = "unix:///var/run/docker.sock";
            log.info("macOS detected, using Unix socket for Docker");
        } else {
            dockerHost = "unix:///var/run/docker.sock";
            log.info("Linux detected, using Unix socket for Docker");
        }

        log.debug("Docker host resolved to: {}", dockerHost);
        return dockerHost;
    }
}
