package com.technokratos.solutionservice.dto;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.solutionservice.model.SolutionEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * DTO for {@link SolutionEntity}
 */
public record SolutionRequest(
        @NotNull ProgrammingLanguage language,
        @NotBlank String code
) {
}