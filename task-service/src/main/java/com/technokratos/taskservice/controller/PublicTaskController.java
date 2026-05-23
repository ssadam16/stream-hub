package com.technokratos.taskservice.controller;

import com.technokratos.common.dto.TaskPublicView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.taskservice.api.PublicTaskApi;
import com.technokratos.taskservice.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicTaskController implements PublicTaskApi {

    private final TaskService taskService;

    @Override
    public Page<TaskShortView> list(TaskDifficulty difficulty, List<String> tags, Pageable pageable) {
        return taskService.listPublic(difficulty, tags, pageable);
    }

    @Override
    public TaskPublicView getById(UUID id) {
        return taskService.getPublicById(id);
    }
}
