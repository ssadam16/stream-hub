package com.technokratos.common.events;

import com.technokratos.common.dto.tests.TestCasePublic;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Request to code-executor-service to run a user's solution against test cases.
 * Sent on topic "code-execution-topic".
 *
 * Note: when admins want hidden tests included, the executor receives the full set;
 * filtering by isHidden happens at the task-service layer before publishing.
 */
@Builder
public record CodeExecutionRequest(
        UUID solutionId,
        UUID taskId,
        UUID userId,
        ProgrammingLanguage language,
        String code,
        int timeLimitMs,
        int memoryLimitMb,
        List<TestCasePublic> testCases
) {}
