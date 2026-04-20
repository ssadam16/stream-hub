package com.technokratos.apigateway.handler;

import com.technokratos.apigateway.dto.ExceptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Ошибки от Keycloak (401, 400, 403)
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ExceptionMessage> handleWebClientResponseException(WebClientResponseException e) {
        log.error("WebClient error: {}, status: {}, response: {}",
                e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());

        if (e.getStatusCode().is4xxClientError()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ExceptionMessage.builder()
                            .message("Invalid credentials or bad request")
                            .exceptionName(e.getClass().getName())
                            .build());
        }

        if (e.getStatusCode().is5xxServerError()) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ExceptionMessage.builder()
                            .message("Invalid credentials or bad request")
                            .exceptionName(e.getClass().getName())
                            .build());
        }

        return ResponseEntity
                .status(e.getStatusCode())
                .body(ExceptionMessage.builder()
                        .message("Invalid credentials or bad request")
                        .exceptionName(e.getClass().getName())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionMessage> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionMessage.builder()
                        .message(e.getMessage())
                        .exceptionName(e.getClass().getName())
                        .build());
    }
}
