package com.technokratos.taskservice.parser;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record TaskImportRequest(
        String externalId,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be less than 255 characters")
        String title,

        @NotBlank(message = "Condition is required")
        String condition,

        @NotNull(message = "Difficulty is required")
        TaskDifficulty difficulty,

        TaskTestType type,


       // @NotNull(message = "Category is required")
       // TODO: SH-15 TaskCategory category,

        @NotNull(message = "Time limit is required")
        @Min(value = 1, message = "Time limit must be > 0")
        @Max(value = 60, message = "Time limit must be <= 60 seconds")
        Integer timeLimit,

        @NotNull(message = "Memory limit is required")
        @Min(value = 16, message = "Memory limit must be >= 16 MB")
        @Max(value = 1024, message = "Memory limit must be <= 1024 MB")
        Integer memoryLimit,

        @NotEmpty(message = "At least one test case is required")
        @Valid
        List<TestCaseImport> testCases
) {
}
