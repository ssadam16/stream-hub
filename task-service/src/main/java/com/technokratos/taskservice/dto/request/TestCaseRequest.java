package com.technokratos.taskservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record TestCaseRequest(
        @NotNull String input,
        @NotNull String expectedOutput,
        boolean isHidden,
        String explanation
) {}
