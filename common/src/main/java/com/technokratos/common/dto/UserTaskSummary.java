package com.technokratos.common.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate projection from solution-service repository: per-task summary
 * of the current user's submissions. Internal — feeds CompletedTaskView.
 */
public record UserTaskSummary(
        UUID taskId,
        Instant lastSolvedAt,
        long attemptsCount,
        long completedCount
) {}
