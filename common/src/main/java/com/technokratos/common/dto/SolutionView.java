package com.technokratos.common.dto;

import com.technokratos.common.dto.tests.TestCaseResult;
import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.SolutionStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full view of a single solution. Includes execution results when available.
 * Source code is filled only when caller is the owner or an admin (the service
 * fills it from MongoDB; otherwise null).
 */
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
