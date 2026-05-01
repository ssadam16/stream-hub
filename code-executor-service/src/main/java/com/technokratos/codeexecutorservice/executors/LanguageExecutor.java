package com.technokratos.codeexecutorservice.executors;

import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.common.enums.ProgrammingLanguage;

public interface LanguageExecutor {
    String getImage();
    String getSourceFileName();
    String[] getCompileCommand();
    String[] getRunCommand();
    LanguageConfig getConfig();
    ProgrammingLanguage getLanguage();
}
