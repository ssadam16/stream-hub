package com.technokratos.codeexecutorservice.configuration;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LanguageConfig {
    private Long memoryMB;
    private Long cpuCount;
    private Integer timeout;
    private String workingDir;
    private Long pidsLimit;
}