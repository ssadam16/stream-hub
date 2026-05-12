package com.technokratos.common.events;

import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.Builder;

import java.util.UUID;

@Builder
public record SolutionCompletedEvent(
        UUID solutionId,
        UUID taskId,
        ProgrammingLanguage language
) {}