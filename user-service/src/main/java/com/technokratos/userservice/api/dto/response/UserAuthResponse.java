package com.technokratos.userservice.api.dto.response;

import lombok.Builder;

@Builder
public record UserAuthResponse(
        String token
) {
}
