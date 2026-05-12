package com.technokratos.taskservice.service;

import com.technokratos.taskservice.mapper.ConsoleTestCaseMapper;
import com.technokratos.taskservice.model.tests.ConsoleTestCase;
import com.technokratos.taskservice.request.TaskCreatingRequest;
import com.technokratos.common.dto.TaskResponse;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.exception.TaskNotFoundException;
import com.technokratos.taskservice.mapper.TaskMapper;
import com.technokratos.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ConsoleTestCaseMapper mapper;

    @Transactional
    public TaskResponse create(TaskCreatingRequest request) {
        log.info("Create task with title={}", request.title());

        Task task = taskMapper.toEntity(request);

        List<ConsoleTestCase> testCases = mapper.toConsoleTestCaseList(request.testCases());
        testCases.forEach(t -> t.setTask(task));
        task.setTestCases(testCases);

        return taskMapper.toTaskResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(UUID id) {
        log.info("Get task with id={}", id);

        TaskResponse taskResponse = taskMapper.toTaskResponse(taskRepository.findById(id)
                .orElseThrow(() -> TaskNotFoundException.byId(id)));

        log.info("Task response: {}", taskResponse);

        return taskResponse;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAll() {
        log.info("Get all tasks");

        return taskMapper.toTaskResponseList(taskRepository.findAll());
    }
}
