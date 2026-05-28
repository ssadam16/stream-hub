package com.technokratos.common.dto;

public record ExecResult(
        String stdout,
        String stderr,
        int exitCode,
        long elapsedMs,
        boolean timedOut
) {}
