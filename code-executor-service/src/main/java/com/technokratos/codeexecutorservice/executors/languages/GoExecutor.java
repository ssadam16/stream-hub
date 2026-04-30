package com.technokratos.codeexecutorservice.executors.languages;

import com.technokratos.codeexecutorservice.configuration.DockerProperties;
import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.codeexecutorservice.executors.LanguageExecutor;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoExecutor implements LanguageExecutor {

    private final DockerProperties properties;

    @Override
    public ProgrammingLanguage getLanguage() {
        return ProgrammingLanguage.GO;
    }

    @Override
    public String getImage() {
        return properties.getImages().getGo();
    }

    @Override
    public String getSourceFileName() {
        return "solution.go";
    }

    @Override
    public String[] getCompileCommand() {
        return new String[]{
                "go", "build",
                "-o", "/workspace/solution",
                "/workspace/solution.go"
        };
    }

    @Override
    public String[] getRunCommand() {
        return new String[]{"/workspace/solution"};
    }

    @Override
    public LanguageConfig getConfig() {
        return LanguageConfig.builder()
                .memoryMB(256L)
                .cpuCount(1L)
                .timeout(10)
                .workingDir("/workspace")
                .build();
    }
}