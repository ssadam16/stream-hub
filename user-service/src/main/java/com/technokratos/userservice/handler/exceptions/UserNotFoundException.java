package com.technokratos.userservice.handler.exceptions;

import java.util.UUID;
import com.technokratos.common.exceptions.*;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(UUID id) {
        super("User with id=%s not found".formatted(id));
    }
}
