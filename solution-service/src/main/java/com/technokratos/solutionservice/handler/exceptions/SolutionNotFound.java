package com.technokratos.solutionservice.handler.exceptions;

import com.technokratos.common.exceptions.NotFoundException;

import java.util.UUID;

public class SolutionNotFound extends NotFoundException {
  public SolutionNotFound(UUID id) {
    super("Solution with id %s not found".formatted(id));
  }
}
