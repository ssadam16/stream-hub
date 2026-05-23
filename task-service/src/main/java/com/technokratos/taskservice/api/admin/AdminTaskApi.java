package com.technokratos.taskservice.api.admin;

import com.technokratos.taskservice.dto.request.AdminTaskCreateRequest;
import com.technokratos.taskservice.dto.request.AdminTaskUpdateRequest;
import com.technokratos.taskservice.dto.request.TestCaseRequest;
import com.technokratos.taskservice.dto.response.admin.AdminSubmissionResponse;
import com.technokratos.taskservice.dto.response.admin.AdminTaskResponse;
import com.technokratos.taskservice.dto.response.admin.TaskStatisticsResponse;
import com.technokratos.taskservice.dto.response.admin.TestCaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "Admin Tasks API", description = "Endpoints for administrators")
@RequestMapping("/api/v1/admin/tasks")
public interface AdminTaskApi {

    @Operation(summary = "Get all tasks (including hidden and draft)")
    @GetMapping
    Page<AdminTaskResponse> getAllTasks(Pageable pageable);

    @Operation(summary = "Get full task details with all test cases")
    @GetMapping("/{id}")
    AdminTaskResponse getTaskById(@PathVariable UUID id);

    @Operation(summary = "Create new task with test cases")
    @PostMapping
    AdminTaskResponse createTask(@Valid @RequestBody AdminTaskCreateRequest request);

    @Operation(summary = "Update existing task")
    @PutMapping("/{id}")
    AdminTaskResponse updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody AdminTaskUpdateRequest request
    );

    @Operation(summary = "Delete task (soft delete)")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTask(@PathVariable UUID id);

    @Operation(summary = "Publish/unpublish task")
    @PatchMapping("/{id}/publish")
    AdminTaskResponse publishTask(
            @PathVariable UUID id,
            @RequestParam boolean published
    );

    @Operation(summary = "Add test case to task")
    @PostMapping("/{id}/test-cases")
    TestCaseResponse addTestCase(
            @PathVariable UUID id,
            @Valid @RequestBody TestCaseRequest request
    );

    @Operation(summary = "Update test case")
    @PutMapping("/{id}/test-cases/{testCaseId}")
    TestCaseResponse updateTestCase(
            @PathVariable UUID id,
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseRequest request
    );

    @Operation(summary = "Delete test case")
    @DeleteMapping("/{id}/test-cases/{testCaseId}")
    ResponseEntity<Void> deleteTestCase(
            @PathVariable UUID id,
            @PathVariable Long testCaseId
    );

    @Operation(summary = "Get all submissions for a task (admin view)")
    @GetMapping("/{id}/submissions")
    Page<AdminSubmissionResponse> getTaskSubmissions(
            @PathVariable UUID id,
            Pageable pageable
    );

    @Operation(summary = "Get submission with full test details (including hidden)")
    @GetMapping("/submissions/{submissionId}/full")
    AdminSubmissionResponse getFullSubmissionDetails(@PathVariable UUID submissionId);

    @Operation(summary = "Re-evaluate submission")
    @PostMapping("/submissions/{submissionId}/re-evaluate")
    AdminSubmissionResponse reEvaluateSubmission(@PathVariable UUID submissionId);

    @Operation(summary = "Get task statistics")
    @GetMapping("/{id}/statistics")
    TaskStatisticsResponse getTaskStatistics(@PathVariable UUID id);

    @Operation(summary = "Bulk import test cases")
    @PostMapping("/{id}/test-cases/bulk")
    List<TestCaseResponse> bulkAddTestCases(
            @PathVariable UUID id,
            @Valid @RequestBody List<TestCaseRequest> requests
    );
}
