package com.technokratos.userservice.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ProcessedEvent(
        UUID eventId,
        Instant processedAt
) { }
