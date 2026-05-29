package com.technokratos.common.events;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

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
