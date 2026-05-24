package com.technokratos.userservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SolvedTaskStatsEntity {
    private UUID accountId;
    private UUID taskId;
    private Instant solvedAt;
}
