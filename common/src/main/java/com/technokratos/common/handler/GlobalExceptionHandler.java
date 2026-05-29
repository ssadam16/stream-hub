package com.technokratos.common.handler;

import com.technokratos.common.handler.exceptions.ExceptionMessage;
import com.technokratos.common.handler.exceptions.ServiceException;
import com.technokratos.common.handler.exceptions.ValidationExceptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

//    @Value("${app.debug.expose-error-details:false}")
//    private boolean exposeErrorDetails;

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    private boolean isDevelopmentProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ExceptionMessage> handleServiceException(ServiceException e) {
        log.debug("Handling ServiceException: {}", e.getMessage());

        return ResponseEntity.status(e.getHttpStatus())
                .body(ExceptionMessage.builder()
                        .exceptionName(e.getClass().getSimpleName())
                        .message(e.getMessage())
                        .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidationExceptionMessage>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {

        List<ValidationExceptionMessage> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationExceptionMessage(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        return ResponseEntity.status(e.getStatusCode()).body(errors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ExceptionMessage handleAllErrors(Exception e) {
        String requestInfo = getRequestInfo();

        log.error("Unhandled exception. Request: {}", requestInfo, e);

        String userMessage = isDevelopmentProfile()
                ? buildDetailedUserMessage(e)
                : buildSafeUserMessage();

        return ExceptionMessage.builder()
                .exceptionName("InternalServerError")
                .message(userMessage)
                .build();
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getRequestInfo() {
        try {
            var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                var request = attributes.getRequest();
                return String.format("%s %s from %s",
                        request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            }
        } catch (Exception ex) {
            log.debug("Could not get request info", ex);
        }
        return "unknown";
    }

    private String buildSafeUserMessage() {
        return "An unexpected error occurred. Please try later";
    }

    private String buildDetailedUserMessage(Exception e) {
        return String.format("Error: %s - %s", e.getClass().getSimpleName(), e.getMessage());
    }
}