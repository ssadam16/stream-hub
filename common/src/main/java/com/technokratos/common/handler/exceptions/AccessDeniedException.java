package com.technokratos.common.handler.exceptions;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends ServiceException {
    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
