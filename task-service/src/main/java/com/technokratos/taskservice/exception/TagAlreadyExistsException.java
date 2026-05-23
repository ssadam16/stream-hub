package com.technokratos.taskservice.exception;

import com.technokratos.common.handler.exceptions.ServiceException;
import org.springframework.http.HttpStatus;

public class TagAlreadyExistsException extends ServiceException {

    public TagAlreadyExistsException(String slug) {
        super("Tag with slug '%s' already exists".formatted(slug), HttpStatus.CONFLICT);
    }
}
