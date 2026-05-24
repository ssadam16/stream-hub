package com.technokratos.common.events;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by solution-service when a solution reaches a final status.
 * Consumed by task-service to increment per-task submission counters
 * (totalSubmissions, successfulSubmissions).
 *
 * Topic: "task.statistics.events"
 */
@Builder
public record TaskSubmissionEvent(
        UUID solutionId,
        UUID taskId,
        UUID userId,
        boolean completed,
        Instant timestamp,
        int points,
        ProgrammingLanguage language,
        TaskDifficulty difficulty
) {}
