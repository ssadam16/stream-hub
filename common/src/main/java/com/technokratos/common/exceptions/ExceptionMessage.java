package com.technokratos.common.exceptions;

import lombok.Builder;

@Builder
public record ExceptionMessage (
        String message,
        String exceptionName
) {
}

