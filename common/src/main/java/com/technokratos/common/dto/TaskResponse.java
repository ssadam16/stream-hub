package com.technokratos.common.dto;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.common.dto.tests.TestCasePayload;

import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String condition,
        TaskDifficulty difficulty,
        UUID authorId,
        TaskTestType type,
        Integer timeLimit,
        Integer memoryLimit,
        List<TestCasePayload> testCases
) {}
