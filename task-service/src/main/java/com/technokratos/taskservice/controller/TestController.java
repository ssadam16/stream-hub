package com.technokratos.taskservice.controller;

import com.technokratos.common.dto.tests.TestCasePayload;
import com.technokratos.taskservice.api.TestApi;
import com.technokratos.taskservice.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TestController implements TestApi {

    private final TestService testService;

    @Override
    public List<TestCasePayload> getAllTestsForTask(UUID taskId) {
        return testService.getAllByTaskId(taskId);
    }
}
