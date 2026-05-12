package com.technokratos.common.exceptions;

import java.util.UUID;

public class SolutionNotFound extends NotFoundException {
  public SolutionNotFound(UUID id) {
    super("Solution with id %s not found".formatted(id));
  }
}
