package com.technokratos.taskservice.api.dto.response;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.common.dto.tests.ConsoleTestCaseResponse;

import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String condition,
        TaskDifficulty difficulty,
        TaskTestType type,
        List<ConsoleTestCaseResponse> testCases
) {}
