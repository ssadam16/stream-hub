package com.technokratos.userservice.handler.exceptions;

import java.util.UUID;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(UUID id) {
        super("User with id=%s not found".formatted(id));
    }
}
