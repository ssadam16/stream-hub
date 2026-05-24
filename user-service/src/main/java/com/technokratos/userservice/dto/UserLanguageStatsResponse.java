package com.technokratos.userservice.dto;


import com.technokratos.common.enums.ProgrammingLanguage;

import lombok.Builder;

import java.util.Map;

@Builder
public record UserLanguageStatsResponse(
        Map<ProgrammingLanguage, Integer> stats
) {
}
