package com.technokratos.codeexecutorservice.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "executor")
@Getter
@Setter
public class ExecutorProperties {
    private int maxConcurrentContainers;
}
