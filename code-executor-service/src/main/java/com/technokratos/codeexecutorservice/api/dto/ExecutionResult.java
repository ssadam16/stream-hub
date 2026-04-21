package com.technokratos.codeexecutorservice.api.dto;

import lombok.Builder;

@Builder
public record ExecutionResult(
        String containerId,
        boolean isSuccess,
        String output
) {
}
