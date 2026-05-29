package com.technokratos.common.dto.tests;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TestCaseFull(
        UUID id,
        String input,
        String expectedOutput,
        boolean isHidden,
        String explanation
) {}
