package com.technokratos.common.dto;

/**
 * Result of a single low-level command execution inside a Docker container.
 * Used by code-executor-service for compile/run steps.
 */
public record ExecResult(
        String stdout,
        String stderr,
        int exitCode,
        long elapsedMs,
        boolean timedOut
) {}
