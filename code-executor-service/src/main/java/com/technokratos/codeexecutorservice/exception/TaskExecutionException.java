package com.technokratos.codeexecutorservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class TaskExecutionException extends RuntimeException {

    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static TaskExecutionException bySolutionId(UUID solutionId, Throwable cause) {
        return new TaskExecutionException("Execution failed for solution with ID: %s. Cause: %s".formatted(solutionId, cause.toString()));
    }
}
