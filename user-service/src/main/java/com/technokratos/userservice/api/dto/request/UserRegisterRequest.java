package com.technokratos.userservice.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for {@link com.technokratos.userservice.entities.UserEntity}
 */
public record UserRegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 5, max = 30, message = "Username must be between 5 and 30 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {}