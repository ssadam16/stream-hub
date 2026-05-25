package com.technokratos.userservice.dto;

import java.util.UUID;

public record AccountInfoResponse(
        UUID accountId,
        String username,
        String email,
        String fio
) {
}
