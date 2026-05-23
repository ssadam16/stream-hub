package com.technokratos.common.dto;

import lombok.Builder;

@Builder
public record SolutionStats(
        long submissionsCount,
        long acceptedCount,
        double acceptanceRate
) {}
