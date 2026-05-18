package com.technokratos.apigateway.handler.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends GatewayException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }
}
