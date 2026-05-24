package com.technokratos.common.events;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record SolutionCompletedEvent(
        UUID solutionId,
        UUID taskId,
        UUID userId,
        ProgrammingLanguage language
) {}