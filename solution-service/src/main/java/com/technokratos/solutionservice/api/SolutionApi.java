package com.technokratos.solutionservice.api;

import com.technokratos.common.dto.CompletedTaskView;
import com.technokratos.common.dto.PageResponse;
import com.technokratos.common.dto.SolutionShortView;
import com.technokratos.common.dto.SolutionView;
import com.technokratos.solutionservice.dto.SolutionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Solutions")
@RequestMapping("/api/v1/solutions")
public interface SolutionApi {

    @Operation(summary = "Submit solution for a task")
    @PostMapping("/tasks/{taskId}/solve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    UUID submit(
            @RequestBody @Valid SolutionRequest dto,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Get my solutions")
    @GetMapping("/my")
    PageResponse<SolutionShortView> listMine(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Get my completed tasks")
    @GetMapping("/my/completed-tasks")
    List<CompletedTaskView> myCompleted(
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Get solution by id")
    @GetMapping("/{id}")
    SolutionView getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Delete solution")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    );
}