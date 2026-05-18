package com.technokratos.apigateway.handler.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends GatewayException {
    public TokenExpiredException() {
        super(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Refresh token expired or invalid");
    }
}
