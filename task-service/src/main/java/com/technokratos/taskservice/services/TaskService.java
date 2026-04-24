package com.technokratos.taskservice.services;

import com.technokratos.taskservice.api.dto.request.TaskCreatingRequest;
import com.technokratos.common.dto.TaskResponse;
import com.technokratos.taskservice.entities.Task;
import com.technokratos.taskservice.entities.tests.ConsoleTestCase;
import com.technokratos.taskservice.exceptions.TaskNotFoundException;
import com.technokratos.taskservice.mappers.ConsoleTestCaseMapper;
import com.technokratos.taskservice.mappers.TaskMapper;
import com.technokratos.taskservice.repositories.TaskRepository;
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

        Task task = taskMapper.toTask(request);

        List<ConsoleTestCase> testCases = mapper.toConsoleTestCaseList(request.testCases());
        testCases.forEach(t -> t.setTask(task));
        task.setTestCases(testCases);

        return taskMapper.toTaskResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(UUID id) {
        log.info("Get task with id={}", id);

        return taskMapper.toTaskResponse(taskRepository.findById(id)
                .orElseThrow(() -> TaskNotFoundException.byId(id))
        );
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAll() {
        log.info("Get all tasks");

        return taskMapper.toTaskResponseList(taskRepository.findAll());
    }
}
