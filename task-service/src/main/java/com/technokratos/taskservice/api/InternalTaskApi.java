package com.technokratos.taskservice.api;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskShortView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Internal endpoints consumed only by other services (not exposed via gateway
 * for end users). Returns full data including hidden test cases — used by
 * solution-service to send code to executor.
 */
@Tag(name = "Internal Tasks API", description = "Service-to-service endpoints")
@RequestMapping("/internal/v1/tasks")
public interface InternalTaskApi {

    @Operation(summary = "Get task with ALL test cases including hidden (executor uses this)")
    @GetMapping("/{id}")
    TaskAdminView getForExecution(@PathVariable UUID id);

    @Operation(summary = "Batch: short cards for multiple task ids")
    @PostMapping("/batch")
    Map<UUID, TaskShortView> getBatch(@RequestBody List<UUID> ids);
}
