package com.technokratos.taskservice.parser;

import jakarta.validation.constraints.NotBlank;

public record TestCaseImport(
        @NotBlank(message = "Input is required")
        String input,

        @NotBlank(message = "Expected output is required")
        String expectedOutput,

        boolean isHidden
) {
}
