package com.technokratos.common.dto;

import com.technokratos.common.enums.TaskDifficulty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Short task card used in lists and as a snippet inside other views
 * (e.g. completed-tasks list in solution-service).
 */
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
