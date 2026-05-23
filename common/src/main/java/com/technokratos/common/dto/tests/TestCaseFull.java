package com.technokratos.common.dto.tests;

import lombok.Builder;

import java.util.UUID;

/**
 * Full test case info, visible to admins only.
 */
@Builder
public record TestCaseFull(
        UUID id,
        String input,
        String expectedOutput,
        boolean isHidden,
        String explanation
) {}
