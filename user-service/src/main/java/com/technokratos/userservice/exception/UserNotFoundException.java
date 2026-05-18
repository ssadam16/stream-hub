package com.technokratos.userservice.exception;

import java.util.UUID;

import com.technokratos.common.handler.exceptions.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(UUID id) {
        super("User with id=%s not found".formatted(id));
    }
}
