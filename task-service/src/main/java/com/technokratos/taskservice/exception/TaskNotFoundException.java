package com.technokratos.taskservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(String message) {
        super(message);
    }

    public TaskNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static TaskNotFoundException byId(UUID id) {
        return new TaskNotFoundException("Task not found with ID: %s".formatted(id));
    }

    public static TaskNotFoundException byTitle(String title) {
        return new TaskNotFoundException("Task not found with title: %s".formatted(title));
    }
}
