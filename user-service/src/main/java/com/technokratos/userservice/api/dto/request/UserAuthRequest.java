package com.technokratos.userservice.api.dto.request;

import lombok.Builder;

@Builder
public record UserAuthRequest(
        String email,
        String password
) {
}
