package com.technokratos.taskservice.api;

import com.technokratos.common.dto.TaskPublicView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.enums.TaskDifficulty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Public Tasks API", description = "Tasks visible to authenticated users (PUBLISHED only)")
@RequestMapping("/api/v1/tasks")
public interface PublicTaskApi {

    @Operation(summary = "List published tasks (filter by difficulty / tags)")
    @GetMapping
    Page<TaskShortView> list(
            @RequestParam(required = false) TaskDifficulty difficulty,
            @RequestParam(required = false) List<String> tags,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    );

    @Operation(summary = "Get task by id (non-hidden test cases only)")
    @GetMapping("/{id}")
    TaskPublicView getById(@PathVariable UUID id);
}
