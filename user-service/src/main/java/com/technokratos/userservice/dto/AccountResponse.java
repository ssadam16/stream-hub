package com.technokratos.userservice.dto;

import lombok.Builder;

import java.time.Instant;

import java.util.UUID;

@Builder
public record AccountResponse(
         UUID accountId,
         String email,
         String username,
         String fio,
         long totalSubmissions,
         long acceptedSubmissions,
         long solvedTasksCount,
         long totalPoints,
         double acceptance,
         Instant lastActive,
         long currentStreak,
         long maxStreak,
         UserDifficultyStatsResponse userDifficultyStats,
         UserLanguageStatsResponse userLanguageStats
) {
}
