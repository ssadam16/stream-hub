package com.technokratos.solutionservice.handler;

import com.technokratos.common.exceptions.ExceptionMessage;
import com.technokratos.common.exceptions.ServiceException;
import com.technokratos.common.exceptions.ValidationExceptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ExceptionMessage> handleServiceException(ServiceException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ExceptionMessage.builder()
                        .exceptionName(e.getClass().getSimpleName())
                        .message(e.getMessage())
                        .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidationExceptionMessage>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
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
    public ExceptionMessage handeAllErrors(Exception e) {
        return ExceptionMessage.builder()
                .message(e.getMessage())
                .exceptionName(e.getClass().getSimpleName())
                .build();
    }
}
