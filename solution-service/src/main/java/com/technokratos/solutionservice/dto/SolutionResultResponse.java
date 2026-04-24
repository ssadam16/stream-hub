package com.technokratos.solutionservice.dto;

import com.technokratos.common.dto.tests.TestCaseResult;
import com.technokratos.common.enums.SolutionStatus;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SolutionResultResponse(
        UUID solutionId,
        SolutionStatus status,
        Long passedTests,
        Integer totalTests,
        Long totalTimeMs,
        List<TestCaseResult> testResults
) { }
