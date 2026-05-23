package com.technokratos.taskservice.dto.request;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record AdminTaskCreateRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        @NotBlank(message = "Condition is required")
        String condition,

        @NotNull(message = "Difficulty is required")
        TaskDifficulty difficulty,

        @NotNull(message = "Type is required")
        TaskTestType type,

        @NotNull(message = "Points are required")
        @Min(value = 0, message = "Points must be >= 0")
        Integer points,

        @NotNull(message = "Time limit is required")
        @Min(value = 1) @Max(value = 60_000)
        Integer timeLimitMs,

        @NotNull(message = "Memory limit is required")
        @Min(value = 16) @Max(value = 1024)
        Integer memoryLimitMb,

        Set<String> tagSlugs,

        @NotEmpty(message = "At least one test case is required")
        @Valid
        List<TestCaseRequest> testCases
) {}
