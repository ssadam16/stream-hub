package com.technokratos.apigateway.dto;

import lombok.Builder;

@Builder
public record AuthResponse (
     String accessToken,
     String refreshToken,
     Integer expiresIn,
     String tokenType
) {}
