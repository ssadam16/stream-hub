package com.technokratos.codeexecutorservice.configuration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LanguageConfig {
    private Long memoryMB;
    private Long cpuCount;
    private Integer timeout;
    private String workingDir;
    private String[] command;
}