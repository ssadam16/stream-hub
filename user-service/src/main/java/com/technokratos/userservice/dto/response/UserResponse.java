package com.technokratos.userservice.dto.response;

import lombok.Builder;

@Builder
public record UserResponse(
        String id,
        String username,
        String email,
        String keycloakId
) {
}