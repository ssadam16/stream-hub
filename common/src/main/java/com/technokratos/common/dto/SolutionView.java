package com.technokratos.common.dto;

import com.technokratos.common.dto.tests.TestCaseResult;
import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.SolutionStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record SolutionView(
        UUID id,
        UUID userId,
        UUID taskId,
        ProgrammingLanguage language,
        SolutionStatus status,
        Long passedTests,
        Integer totalTests,
        Long totalTimeMs,
        List<TestCaseResult> testResults,
        String code,
        Instant createdAt,
        Instant updatedAt
) {}
