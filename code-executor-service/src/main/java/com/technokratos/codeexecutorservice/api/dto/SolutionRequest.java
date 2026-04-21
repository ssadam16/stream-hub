package com.technokratos.codeexecutorservice.api.dto;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskTestType;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SolutionRequest(
        String code,
        ProgrammingLanguage language,
        UUID solutionId,
        TaskTestType testType,
        //todo: перенести TestCaseResponse в common и использовать его
        List<Object> tests
) {}
