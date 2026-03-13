package com.technokratos.common.dto;

import lombok.Builder;

@Builder
public record TokenValidationRequest (
        String token
) {}
