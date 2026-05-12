package com.technokratos.taskservice.request.tests;

import lombok.*;

@Builder
public record ConsoleTestCaseCreatingRequest(
        boolean isHidden,
        String input,
        String expectedOutput
) {
}