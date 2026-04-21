package com.technokratos.apigateway.dto;

import lombok.Builder;

@Builder
public record ErrorJwtResponse(
        String exceptionName,
        String message
) {
}
