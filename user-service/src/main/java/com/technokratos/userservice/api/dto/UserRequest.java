package com.technokratos.userservice.api.dto;

/**
 * DTO for {@link com.technokratos.userservice.entities.UserEntity}
 */
public record UserRequest(String username, String email, String password) {
}