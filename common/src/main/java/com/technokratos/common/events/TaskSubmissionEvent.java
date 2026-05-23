package com.technokratos.common.events;

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
        boolean accepted,
        Instant timestamp
) {}
