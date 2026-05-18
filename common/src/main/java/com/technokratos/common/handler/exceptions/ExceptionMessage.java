package com.technokratos.common.handler.exceptions;

import lombok.Builder;

@Builder
public record ExceptionMessage (
        String message,
        String exceptionName
) {}

