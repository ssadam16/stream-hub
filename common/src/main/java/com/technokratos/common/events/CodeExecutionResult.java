package com.technokratos.common.events;

import com.technokratos.common.dto.tests.TestCaseResult;
import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.enums.TaskDifficulty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CodeExecutionResult(
        UUID solutionId,
        SolutionStatus status,           // COMPLETED / COMPILATION_ERROR / RUNTIME_ERROR / TIMEOUT / INTERNAL_ERROR
        long passedTests,
        int totalTests,
        List<TestCaseResult> testResults,
        Long totalTimeMs,
        TaskDifficulty difficulty,
        ProgrammingLanguage language,
        int points,
        String errorMessage              // для compile errors и internal errors
) {

}
