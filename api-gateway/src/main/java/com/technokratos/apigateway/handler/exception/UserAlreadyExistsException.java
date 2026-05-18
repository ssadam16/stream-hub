package com.technokratos.apigateway.handler.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends GatewayException {
    public UserAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS",
                "User with email %s already exists".formatted(email));
    }
}
