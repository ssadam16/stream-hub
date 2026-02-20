package com.technokratos.userservice.api.dto;

import com.technokratos.userservice.entities.UserEntity;

import java.util.UUID;

/**
 * DTO for {@link UserEntity}
 */

public record UserResponse(UUID id, String username, String email, String hashPassword) {
}