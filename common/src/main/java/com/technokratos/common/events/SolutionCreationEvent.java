package com.technokratos.common.events;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.SolutionStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record SolutionCreationEvent (
        UUID solutionId,
        UUID taskId,
        ProgrammingLanguage language,
        String code,
        SolutionStatus status,
        Instant createdAt
) {
}
