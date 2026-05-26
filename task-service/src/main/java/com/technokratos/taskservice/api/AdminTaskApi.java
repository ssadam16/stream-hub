package com.technokratos.taskservice.api;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import com.technokratos.taskservice.dto.request.AdminTaskCreateRequest;
import com.technokratos.taskservice.dto.request.AdminTaskUpdateRequest;
import com.technokratos.taskservice.parser.ImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin Tasks API", description = "Endpoints for administrators")
@RequestMapping("/api/v1/admin/tasks")
public interface AdminTaskApi {

    @Operation(summary = "List tasks (admin sees drafts and archived too)")
    @GetMapping
    Page<TaskAdminView> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskDifficulty difficulty,
            @RequestParam(required = false) List<String> tags,
            Pageable pageable
    );

    @Operation(summary = "Get task with all test cases (including hidden)")
    @GetMapping("/{id}")
    TaskAdminView getById(@PathVariable UUID id);

    @Operation(summary = "Create new task (starts as DRAFT)")
    @PostMapping
    ResponseEntity<TaskAdminView> create(
            @Valid @RequestBody AdminTaskCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Partial update")
    @PatchMapping("/{id}")
    TaskAdminView update(@PathVariable UUID id, @Valid @RequestBody AdminTaskUpdateRequest request);

    @Operation(summary = "Publish or unpublish (DRAFT <-> PUBLISHED)")
    @PatchMapping("/{id}/publish")
    TaskAdminView publish(@PathVariable UUID id, @RequestParam boolean publish);

    @Operation(summary = "Delete task")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);

    @Operation(summary = "Bulk import tasks from JSON/YAML file")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ImportResult> importTasks(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    );
}
