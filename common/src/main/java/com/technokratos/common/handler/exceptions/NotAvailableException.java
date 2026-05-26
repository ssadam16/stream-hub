package com.technokratos.common.handler.exceptions;

import org.springframework.http.HttpStatus;

public class NotAvailableException extends ServiceException {
    public NotAvailableException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
