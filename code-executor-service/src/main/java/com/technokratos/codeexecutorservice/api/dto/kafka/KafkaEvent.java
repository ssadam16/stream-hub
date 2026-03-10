package com.technokratos.codeexecutorservice.api.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public abstract class KafkaEvent {
    protected Instant createdAt;
    protected Instant receivedAt;
    protected String message;
}
