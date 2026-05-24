package com.technokratos.userservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AccountEntity {
    private UUID accountId;
    private String username;
    private String email;
    private String fio;
    private long totalSubmissions;
    private long completedSubmissions;
    private long solvedTasksCount;
    private long totalPoints;
    private Instant lastActive;
    private long currentStreak;
    private long maxStreak;
    Instant createdAt;
    Instant updatedAt;
}