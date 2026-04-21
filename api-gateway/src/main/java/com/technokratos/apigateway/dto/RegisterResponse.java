package com.technokratos.apigateway.dto;

import lombok.Builder;

@Builder
public record RegisterResponse(
        String id,
        String username,
        String email,
        String keycloakId
) {}
