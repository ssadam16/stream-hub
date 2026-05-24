package com.technokratos.userservice.dto;


import com.technokratos.common.enums.TaskDifficulty;
import lombok.Builder;

import java.util.Map;

@Builder
public record UserDifficultyStatsResponse(
        Map<TaskDifficulty, Integer> stats
) {
}
