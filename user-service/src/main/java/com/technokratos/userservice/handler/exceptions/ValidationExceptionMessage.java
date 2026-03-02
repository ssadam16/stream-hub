package com.technokratos.userservice.handler.exceptions;


public record ValidationExceptionMessage (
        String field,
        String message
) {
}
