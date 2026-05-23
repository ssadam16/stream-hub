package com.technokratos.taskservice.controller;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.taskservice.api.InternalTaskApi;
import com.technokratos.taskservice.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InternalTaskController implements InternalTaskApi {

    private final TaskService taskService;

    @Override
    public TaskAdminView getForExecution(UUID id) {
        return taskService.getAdminById(id);
    }

    @Override
    public Map<UUID, TaskShortView> getBatch(List<UUID> ids) {
        return taskService.shortViewBatch(ids);
    }
}
