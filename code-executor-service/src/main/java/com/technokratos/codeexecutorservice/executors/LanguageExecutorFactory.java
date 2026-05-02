package com.technokratos.codeexecutorservice.executors;

import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LanguageExecutorFactory {

    private final Map<ProgrammingLanguage, LanguageExecutor> executors;

    public LanguageExecutorFactory(List<LanguageExecutor> executors) {
        this.executors = executors.stream()
                .collect(Collectors.toMap(
                        LanguageExecutor::getLanguage,
                        Function.identity()
                ));

        log.info("Registered language executors: {}", this.executors.keySet());
    }

    public LanguageExecutor get(ProgrammingLanguage language) {
        LanguageExecutor executor = executors.get(language);
        if (executor == null) {
            throw new UnsupportedOperationException("Language not supported: " + language);
        }
        return executor;
    }
}
