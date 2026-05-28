package com.technokratos.common.dto;

import java.time.Instant;
import java.util.UUID;

public record UserTaskSummary(
        UUID taskId,
        Instant lastSolvedAt,
        long attemptsCount,
        long completedCount
) {}
