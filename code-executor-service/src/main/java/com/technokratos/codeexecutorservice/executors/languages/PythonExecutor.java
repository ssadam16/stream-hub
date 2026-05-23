package com.technokratos.codeexecutorservice.executors.languages;

import com.technokratos.codeexecutorservice.configuration.DockerProperties;
import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.codeexecutorservice.executors.LanguageExecutor;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PythonExecutor implements LanguageExecutor {

    private final DockerProperties properties;

    @Override
    public ProgrammingLanguage getLanguage() {
        return ProgrammingLanguage.PYTHON;
    }

    @Override
    public String getImage() {
        return properties.getImages().getPython();
    }

    @Override
    public String getSourceFileName() {
        return "solution.py";
    }

    @Override
    public String[] getCompileCommand() {
        return null;
    }

    @Override
    public String[] getRunCommand() {
        return new String[]{"python", "-u", "/workspace/solution.py"};
    }

    @Override
    public LanguageConfig getConfig() {
        return LanguageConfig.builder()
                .cpuCount(1L)
                .workingDir("/workspace")
                .build();
    }
}
