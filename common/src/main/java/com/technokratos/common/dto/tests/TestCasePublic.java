package com.technokratos.common.dto.tests;

import lombok.Builder;

import java.util.UUID;

/**
 * Test case for code execution and public task views.
 * Contains only what executor needs and user sees.
 * For admin view see {@link TestCaseFull}.
 */
@Builder
public record TestCasePublic(
        UUID id,
        String input,
        String expectedOutput
) {}
