package com.technokratos.taskservice.api.dto.request.tests;

import jakarta.validation.constraints.Max;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Builder
public record ConsoleTestCaseCreatingRequest(
        @Max(value = 5, message = "There must be max 5 test cases")
         short order,
         boolean isExample,
         String input,
         String expectedOutput
) {}