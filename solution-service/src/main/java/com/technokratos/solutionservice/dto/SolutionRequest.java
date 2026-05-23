package com.technokratos.solutionservice.dto;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.solutionservice.model.SolutionEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for {@link SolutionEntity}
 */
public record SolutionRequest(
        @NotNull ProgrammingLanguage language,
        @NotBlank String code
) {
}