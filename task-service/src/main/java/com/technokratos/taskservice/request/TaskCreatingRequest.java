package com.technokratos.taskservice.request;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.request.tests.ConsoleTestCaseCreatingRequest;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record TaskCreatingRequest(
        String title,

        @Size(max = 2047, message = "task's description max length is 2047 symbols")
        String condition,

        TaskDifficulty difficulty,

        @NotNull
        UUID authorId,

        TaskTestType type,

        @Min(value = 1, message = "Time limit must be at least 1")
        Integer timeLimit,

        @Min(value = 1, message = "Memory limit must be at least 1")
        Integer memoryLimit,

        List<ConsoleTestCaseCreatingRequest> testCases
) {}