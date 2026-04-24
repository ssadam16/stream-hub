package com.technokratos.common.dto.tests;

import com.technokratos.common.enums.ErrorType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TestCaseResult(
        UUID testCaseId,
        boolean passed,
        String actualOutput,
        long executionTimeMs,
        ErrorType errorType
) {
}
