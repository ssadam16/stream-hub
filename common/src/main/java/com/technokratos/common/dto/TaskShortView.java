package com.technokratos.common.dto;

import com.technokratos.common.enums.TaskDifficulty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record TaskShortView(
        UUID id,
        String title,
        String slug,
        TaskDifficulty difficulty,
        int points,
        TaskStats stats,
        List<TagDto> tags
) {}
