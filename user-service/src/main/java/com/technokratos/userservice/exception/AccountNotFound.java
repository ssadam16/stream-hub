package com.technokratos.userservice.exception;

import com.technokratos.common.handler.exceptions.NotFoundException;

import java.util.UUID;

public class AccountNotFound extends NotFoundException {
    public AccountNotFound(UUID accountId) {
        super("Account not found with id %s".formatted(accountId));
    }
}
