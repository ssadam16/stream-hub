package com.technokratos.taskservice.api.controllers;

import com.technokratos.taskservice.api.dto.request.TaskCreatingRequest;
import com.technokratos.taskservice.api.dto.response.TaskResponse;
import com.technokratos.taskservice.services.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController implements TaskApi {

    private final TaskService taskService;

    @Override
    public ResponseEntity<TaskResponse> createTask(TaskCreatingRequest request) {
        return new ResponseEntity<>(taskService.create(request), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return null;
    }

    @Override
    public ResponseEntity<TaskResponse> getTaskById(UUID id) {
        return ResponseEntity.ok(taskService.getById(id));
    }
}
