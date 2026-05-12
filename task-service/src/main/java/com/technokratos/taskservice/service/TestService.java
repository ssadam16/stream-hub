package com.technokratos.taskservice.service;

import com.technokratos.common.dto.tests.TestCasePayload;
import com.technokratos.taskservice.model.tests.ConsoleTestCase;
import com.technokratos.taskservice.mapper.ConsoleTestCaseMapper;
import com.technokratos.taskservice.repository.ConsoleTestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestService {

    private final ConsoleTestCaseRepository testCaseRepository;
    private final ConsoleTestCaseMapper testCaseMapper;

    public List<TestCasePayload> getAllByTaskId(UUID taskId) {
        log.info("Getting all tests for task {}", taskId);
        List<ConsoleTestCase> allByTaskId = testCaseRepository.getAllByTask_Id(taskId);

        return testCaseMapper.toConsoleTestCaseResponse(allByTaskId);
    }

}
