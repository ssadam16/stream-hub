package com.technokratos.apigateway.dto;

import lombok.Builder;

@Builder
public record ExceptionMessage (
        String message,
        String exceptionName
) {}

