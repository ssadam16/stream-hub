package com.technokratos.common.dto;

import lombok.Builder;

@Builder
public record TaskStats(
        long totalSubmissions,
        long successfulSubmissions,
        double acceptanceRate
) {
    public TaskStats(long total, long success) {
        this(total, success, total == 0 ? 0.0 : Math.round((success * 10000.0) / total) / 100.0);
    }
}
