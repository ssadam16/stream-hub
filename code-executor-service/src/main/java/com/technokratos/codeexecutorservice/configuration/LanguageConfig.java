package com.technokratos.codeexecutorservice.configuration;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LanguageConfig {
    private Long cpuCount;
    private String workingDir;
    private Long pidsLimit;
}