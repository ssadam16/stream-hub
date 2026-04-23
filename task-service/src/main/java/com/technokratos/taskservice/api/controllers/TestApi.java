package com.technokratos.taskservice.api.controllers;

import com.technokratos.common.dto.tests.TestCasePayload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/tests")
public interface TestApi {

    @GetMapping("/tasks/{taskId}")
    List<TestCasePayload> getAllTestsForTask(@PathVariable UUID taskId);

}
