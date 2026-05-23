package com.technokratos.common.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record CompletedTaskView(
        TaskShortView task,
        List<SolutionShortView> solutions,
        Instant lastSolvedAt,
        long attemptsCount
) {}
