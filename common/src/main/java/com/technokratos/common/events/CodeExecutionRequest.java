package com.technokratos.common.events;

import com.technokratos.common.dto.tests.TestCasePayload;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CodeExecutionRequest(
        UUID solutionId,
        UUID taskId,
        UUID userId,
        ProgrammingLanguage language,
        String code,
        Integer timeLimit,
        Integer memoryLimit,
        List<TestCasePayload> testCases
) {}