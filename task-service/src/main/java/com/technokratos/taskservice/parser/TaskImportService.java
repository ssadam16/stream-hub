package com.technokratos.taskservice.parser;

import com.technokratos.common.enums.TaskStatus;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.exception.ImportValidationException;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.model.TestCase;
import com.technokratos.taskservice.repository.TaskRepository;
import com.technokratos.taskservice.util.SlugUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskImportService {

    private final TaskRepository taskRepository;
    private final Validator validator;

    public ImportResult importTasks(List<TaskImportRequest> requests, UUID adminId) {
        log.info("Importing {} tasks by admin={}", requests.size(), adminId);
        validateAll(requests);

        int imported = 0;
        int updated = 0;

        for (TaskImportRequest req : requests) {
            Optional<Task> existing = req.externalId() == null
                    ? Optional.empty()
                    : taskRepository.findByExternalId(req.externalId());

            if (existing.isPresent()) {
                applyToEntity(req, existing.get());
                existing.get().setAuthorId(adminId);
                updated++;
            } else {
                Task task = buildNew(req, adminId);
                taskRepository.save(task);
                imported++;
            }
        }

        return ImportResult.builder().imported(imported).updated(updated).message("OK").build();
    }

    private Task buildNew(TaskImportRequest req, UUID adminId) {
        Task task = Task.builder()
                .externalId(req.externalId())
                .title(req.title())
                .slug(uniqueSlug(req.title()))
                .condition(req.condition())
                .difficulty(req.difficulty())
                .type(req.type() == null ? TaskTestType.CONSOLE_IO : req.type())
                .status(TaskStatus.PUBLISHED)
                .points(0)
                .timeLimitMs(req.timeLimit() * 1000)
                .memoryLimitMb(req.memoryLimit())
                .authorId(adminId)
                .build();
        for (TestCaseImport tc : req.testCases()) {
            task.getTestCases().add(TestCase.builder()
                    .task(task)
                    .input(tc.input())
                    .expectedOutput(tc.expectedOutput())
                    .isHidden(tc.isHidden())
                    .build());
        }
        return task;
    }

    private void applyToEntity(TaskImportRequest req, Task task) {
        task.setTitle(req.title());
        task.setCondition(req.condition());
        task.setDifficulty(req.difficulty());
        if (req.type() != null) {
            task.setType(req.type());
        }
        task.setTimeLimitMs(req.timeLimit() * 1000);
        task.setMemoryLimitMb(req.memoryLimit());

        task.getTestCases().clear();
        for (TestCaseImport tc : req.testCases()) {
            task.getTestCases().add(TestCase.builder()
                    .task(task)
                    .input(tc.input())
                    .expectedOutput(tc.expectedOutput())
                    .isHidden(tc.isHidden())
                    .build());
        }
    }

    private String uniqueSlug(String title) {
        String base = SlugUtil.slugify(title);
        String candidate = base;
        int n = 2;
        while (taskRepository.existsBySlug(candidate)) {
            candidate = base + "-" + n++;
        }
        return candidate;
    }

    private void validateAll(List<TaskImportRequest> requests) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            Set<ConstraintViolation<TaskImportRequest>> violations = validator.validate(requests.get(i));
            for (ConstraintViolation<TaskImportRequest> v : violations) {
                errors.add("Task[%d].%s: %s".formatted(i, v.getPropertyPath(), v.getMessage()));
            }
        }
        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }
    }
}
