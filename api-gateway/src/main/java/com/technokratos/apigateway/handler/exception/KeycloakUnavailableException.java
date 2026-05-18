package com.technokratos.apigateway.handler.exception;

import org.springframework.http.HttpStatus;

public class KeycloakUnavailableException extends GatewayException {
    public KeycloakUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_SERVICE_UNAVAILABLE",
                "Authentication service is temporarily unavailable");
    }
}
