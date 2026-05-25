package com.technokratos.userservice.model;

import com.technokratos.common.enums.TaskDifficulty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class UserDifficultyStatsEntity {
    private UUID accountId;
    private TaskDifficulty difficulty;
    private int solvedCount;
}
