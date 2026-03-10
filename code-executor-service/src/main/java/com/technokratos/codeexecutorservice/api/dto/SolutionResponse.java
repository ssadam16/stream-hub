package com.technokratos.codeexecutorservice.api.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SolutionResponse(
        UUID solutionId,
        //todo: перенести TestCaseResponse в common и использовать его
        List<Object> tests
) {}
