package com.technokratos.common.dto;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.SolutionStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record SolutionShortView(
        UUID id,
        UUID taskId,
        ProgrammingLanguage language,
        SolutionStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
