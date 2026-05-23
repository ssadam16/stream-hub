package com.technokratos.common.dto;

import com.technokratos.common.dto.tests.TestCaseFull;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import com.technokratos.common.enums.TaskTestType;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin view of a task — includes draft/archived tasks and all test cases
 * (including hidden). Returned by /api/v1/admin/tasks/**.
 */
@Builder
public record TaskAdminView(
        UUID id,
        String title,
        String slug,
        String condition,
        TaskDifficulty difficulty,
        TaskStatus status,
        TaskTestType type,
        int points,
        int timeLimitMs,
        int memoryLimitMb,
        UUID authorId,
        TaskStats stats,
        List<TagDto> tags,
        List<TestCaseFull> testCases,
        Instant createdAt,
        Instant updatedAt
) {}
