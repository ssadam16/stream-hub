package com.technokratos.taskservice.controller;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import com.technokratos.taskservice.api.AdminTaskApi;
import com.technokratos.taskservice.dto.request.AdminTaskCreateRequest;
import com.technokratos.taskservice.dto.request.AdminTaskUpdateRequest;
import com.technokratos.taskservice.parser.FileParser;
import com.technokratos.taskservice.parser.FileParserFactory;
import com.technokratos.taskservice.parser.ImportResult;
import com.technokratos.taskservice.parser.TaskImportRequest;
import com.technokratos.taskservice.parser.TaskImportService;
import com.technokratos.taskservice.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('stream_hub.admin')")
public class AdminTaskController implements AdminTaskApi {

    private final TaskService taskService;
    private final FileParserFactory fileParserFactory;
    private final TaskImportService taskImportService;

    @Override
    public Page<TaskAdminView> list(TaskStatus status, TaskDifficulty difficulty,
                                    List<String> tags, Pageable pageable) {
        return taskService.listAdmin(status, difficulty, tags, pageable);
    }

    @Override
    public TaskAdminView getById(UUID id) {
        return taskService.getAdminById(id);
    }

    @Override
    public ResponseEntity<TaskAdminView> create(AdminTaskCreateRequest request, Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, UUID.fromString(jwt.getSubject())));
    }

    @Override
    public TaskAdminView update(UUID id, AdminTaskUpdateRequest request) {
        return taskService.update(id, request);
    }

    @Override
    public TaskAdminView publish(UUID id, boolean publish) {
        return taskService.publish(id, publish);
    }

    @Override
    public ResponseEntity<Void> delete(UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ImportResult> importTasks(MultipartFile file, UUID adminId) {
        FileParser parser = fileParserFactory.getParser(Objects.requireNonNull(file.getOriginalFilename()));
        List<TaskImportRequest> tasks;
        try {
            tasks = parser.parse(file);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ImportResult(0, 0, "Failed to parse file: " + e.getMessage()));
        }
        return ResponseEntity.ok(taskImportService.importTasks(tasks, adminId));
    }
}
