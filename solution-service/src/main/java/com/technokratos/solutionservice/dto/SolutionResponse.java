package com.technokratos.solutionservice.dto;

import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.solutionservice.model.SolutionEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link SolutionEntity}
 */
public record SolutionResponse(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        String language,
        UUID userId,
        UUID taskId,
        SolutionStatus status,
        String code
) {
}