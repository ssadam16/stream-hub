package com.technokratos.taskservice.exception;

import com.technokratos.common.handler.exceptions.ClientException;
import lombok.Getter;

import java.util.List;

@Getter
public class ImportValidationException extends ClientException {

    private final List<String> errors;

    public ImportValidationException(List<String> errors) {
        super("Failed to import file. Errors: %s".formatted(errors));
        this.errors = errors;
    }

}
