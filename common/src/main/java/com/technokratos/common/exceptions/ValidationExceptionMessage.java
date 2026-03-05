package com.technokratos.common.exceptions;

public record ValidationExceptionMessage (
        String field,
        String message
) {}

