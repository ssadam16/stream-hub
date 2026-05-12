package com.technokratos.taskservice.parser;

import com.technokratos.taskservice.exception.ImportValidationException;
import com.technokratos.taskservice.mapper.TaskMapper;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.repository.TaskRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskImportService  {

    private final TaskMapper taskMapper;
    private final TaskRepository taskRepository;
    private final Validator validator;


    public ImportResult importTasks(List<TaskImportRequest> requests, UUID userId) {
        log.info("Try to parse bulk import requests. Size: {}", requests.size());

        List<String> errors = new ArrayList<>();

        validate(errors, requests);

        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }

        int imported = 0;
        int updated = 0;

        for (TaskImportRequest request : requests) {
            if (request.externalId() != null) {
                Optional<Task> existing =
                        taskRepository.findByExternalId(request.externalId());
                if (existing.isPresent()) {
                    Task task = existing.get();
                    task.setAuthorId(userId);



                    taskMapper.updateFromRequest(request, task);
                    task.getTestCases().forEach(tc -> tc.setTask(task));
                    taskRepository.save(task);
                    updated++;
                    continue;
                }
            }
            Task entity = taskMapper.toEntity(request);
            entity.setAuthorId(userId);
            entity.getTestCases().forEach(tc -> tc.setTask(entity));
            taskRepository.save(entity);
            imported++;
        }

        return ImportResult.builder()
                .imported(imported)
                .updated(updated)
                .message("OK")
                .build();
        
    }

    void validate(List<String> errors, List<TaskImportRequest> requests) {
        for (int i = 0; i < requests.size(); i++) {
            Set<ConstraintViolation<TaskImportRequest>> violations =
                    validator.validate(requests.get(i));

            for (ConstraintViolation<TaskImportRequest> violation : violations) {
                errors.add("Task[%d].%s: %s".formatted(
                        i,
                        violation.getPropertyPath(),
                        violation.getMessage()
                ));
            }
        }
    }
}
