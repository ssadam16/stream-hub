package com.technokratos.taskservice.dto.request;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Set;

@Builder
public record AdminTaskUpdateRequest(

        @Size(max = 255)
        String title,

        String condition,

        TaskDifficulty difficulty,

        TaskStatus status,

        @Min(value = 0)
        Integer points,

        @Min(value = 1) @Max(value = 60_000)
        Integer timeLimitMs,

        @Min(value = 16) @Max(value = 1024)
        Integer memoryLimitMb,

        Set<String> tagSlugs
) {}
