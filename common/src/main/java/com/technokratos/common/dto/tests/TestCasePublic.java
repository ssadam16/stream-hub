package com.technokratos.common.dto.tests;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TestCasePublic(
        UUID id,
        String input,
        String expectedOutput
) {}
