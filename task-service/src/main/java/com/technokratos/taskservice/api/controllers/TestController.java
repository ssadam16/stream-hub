package com.technokratos.taskservice.api.controllers;

import com.technokratos.common.dto.tests.ConsoleTestCaseResponse;
import com.technokratos.taskservice.services.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TestController implements TestApi {

    private final TestService testService;

    @Override
    public List<ConsoleTestCaseResponse> getAllTestsForTask(UUID taskId) {
        return testService.getAllByTaskId(taskId);
    }
}
