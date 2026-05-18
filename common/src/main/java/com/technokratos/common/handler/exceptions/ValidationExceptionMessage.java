package com.technokratos.common.handler.exceptions;

public record ValidationExceptionMessage (
        String field,
        String message
) {}

