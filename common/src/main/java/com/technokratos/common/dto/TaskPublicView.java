package com.technokratos.common.dto;

import com.technokratos.common.dto.tests.TestCasePublic;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public view of a single task for users — only non-hidden test cases.
 * Returned by /api/v1/tasks/{id}.
 * Also consumed internally by solution-service for code execution
 * (testCases are the public examples; full set is fetched separately).
 */
@Builder
public record TaskPublicView(
        UUID id,
        String title,
        String slug,
        String condition,
        TaskDifficulty difficulty,
        TaskTestType type,
        int points,
        int timeLimitMs,
        int memoryLimitMb,
        TaskStats stats,
        List<TagDto> tags,
        List<TestCasePublic> examples,
        Instant createdAt
) {}
