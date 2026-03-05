package com.technokratos.apigateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record TokenValidationRequest(
        @NotBlank(message = "Token is required")
        String token
) {}
