// api-gateway/exception/GatewayException.java
package com.technokratos.apigateway.handler.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GatewayException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public GatewayException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

