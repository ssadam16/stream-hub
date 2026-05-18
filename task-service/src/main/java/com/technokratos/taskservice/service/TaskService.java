package com.technokratos.taskservice.service;

import com.technokratos.taskservice.mapper.ConsoleTestCaseMapper;
import com.technokratos.taskservice.model.tests.ConsoleTestCase;
import com.technokratos.taskservice.request.TaskCreatingRequest;
import com.technokratos.common.dto.TaskResponse;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.exception.TaskNotFoundException;
import com.technokratos.taskservice.mapper.TaskMapper;
import com.technokratos.taskservice.repository.TaskRepository;
import com.technokratos.taskservice.request.TaskFilterRequest;
import com.technokratos.taskservice.specification.TaskSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ConsoleTestCaseMapper mapper;

    private static final Set<String> FIELDS_NAME = Arrays.stream(Task.class.getFields()).map(Field::getName).collect(Collectors.toSet());

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

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByFilters(TaskFilterRequest filterRequest) {
        log.info("Get tasks with filters: {}", filterRequest);

        Sort.Direction direction = Sort.Direction.fromString(
                filterRequest.getSortType() != null ? filterRequest.getSortType().toUpperCase() : "DESC"
        );

        String sortField = filterRequest.getSortBy();
        if ("date".equalsIgnoreCase(sortField)) {
            sortField = "createdAt";
        }

        if (!FIELDS_NAME.contains(sortField)) {
            log.warn("illegal value for sortBy");
            throw new IllegalArgumentException("sortBy value is incorrect");
        }

        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(
                filterRequest.getPage() != null ? filterRequest.getPage() : 0,
                filterRequest.getSize() != null ? filterRequest.getSize() : 20,
                sort
        );

        Specification<Task> specification = TaskSpecification.filterBy(
                filterRequest.getDifficulties(),
                filterRequest.getAuthorId(),
                filterRequest.getTypes(),
                filterRequest.getMinRating(),
                filterRequest.getMaxRating(),
                filterRequest.getSearchText()
        );

        Page<Task> taskPage = taskRepository.findAll(specification, pageable);

        return taskPage.map(taskMapper::toTaskResponse);
    }
}
