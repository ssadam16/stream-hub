package com.technokratos.userservice.handler.exceptions;

import lombok.Builder;

@Builder
public record ExceptionMessage (
        String message,

        String exceptionName
) {
}


