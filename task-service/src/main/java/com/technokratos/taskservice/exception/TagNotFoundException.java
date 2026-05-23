package com.technokratos.taskservice.exception;

import com.technokratos.common.handler.exceptions.NotFoundException;

import java.util.UUID;

public class TagNotFoundException extends NotFoundException {

    public TagNotFoundException(String message) {
        super(message);
    }

    public static TagNotFoundException byId(UUID id) {
        return new TagNotFoundException("Tag not found with id: %s".formatted(id));
    }

    public static TagNotFoundException bySlug(String slug) {
        return new TagNotFoundException("Tag not found with slug: %s".formatted(slug));
    }
}
