package com.technokratos.codeexecutorservice.services;

import com.technokratos.codeexecutorservice.configuration.DockerProperties;
import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JavaExecutor implements LanguageExecutor {

    private final DockerProperties properties;

    @Override
    public ProgrammingLanguage getLanguage() {
        return ProgrammingLanguage.JAVA;
    }

    @Override
    public String getImage() {
        return properties.getImages().getJava();
    }

    @Override
    public String getSourceFileName() {
        return "Main.java";
    }

    @Override
    public String[] getCompileCommand() {
        return new String[]{"javac", "/workspace/Main.java"};
    }

    @Override
    public String[] getRunCommand() {
        return new String[]{"java", "-cp", "/workspace", "Main"};
    }

    @Override
    public LanguageConfig getConfig() {
        return LanguageConfig.builder()
                .memoryMB(512L)
                .cpuCount(2L)
                .timeout(30)
                .workingDir("/workspace")
                .build();
    }
}