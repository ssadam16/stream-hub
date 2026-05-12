package com.technokratos.common.dto.tests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TestCasePayload(
        UUID id,
        String input,
        String expectedOutput,
        @JsonProperty("isHidden") boolean isHidden
) {}
